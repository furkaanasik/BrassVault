package com.brassvault.api.service;

import com.brassvault.api.crypto.VaultCryptoService;
import com.brassvault.api.domain.AuditAction;
import com.brassvault.api.domain.Item;
import com.brassvault.api.domain.Team;
import com.brassvault.api.repo.ItemRepository;
import com.brassvault.api.repo.TeamRepository;
import com.brassvault.api.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final TeamRepository teamRepository;
    private final VaultCryptoService crypto;
    private final AuditService auditService;

    public ItemService(ItemRepository itemRepository, TeamRepository teamRepository,
                       VaultCryptoService crypto, AuditService auditService) {
        this.itemRepository = itemRepository;
        this.teamRepository = teamRepository;
        this.crypto = crypto;
        this.auditService = auditService;
    }

    /**
     * The row is inserted first so the generated id exists, then the password is
     * encrypted with that id as AAD and the row updated — all in one transaction.
     */
    @Transactional
    public Item createItem(Long teamId, String title, String username, String password,
                           String url, String notes, Long adminId, String adminEmail, String ip) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Team not found"));
        Item item = new Item();
        item.setTeamId(teamId);
        item.setTitle(title);
        item.setUsername(username);
        item.setUrl(url);
        item.setNotes(notes);
        item.setCreatedBy(adminId);
        item.setEncryptedPassword(new byte[0]);
        item = itemRepository.saveAndFlush(item);
        item.setEncryptedPassword(crypto.encrypt(password, item.getId()));
        item = itemRepository.save(item);
        auditService.record(adminEmail, AuditAction.CREATE_ITEM, title, team.getName(), ip);
        return item;
    }

    @Transactional
    public Item updateItem(Long itemId, String title, String username, String password,
                           String url, String notes, String adminEmail, String ip) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found"));
        item.setTitle(title);
        item.setUsername(username);
        item.setUrl(url);
        item.setNotes(notes);
        if (password != null && !password.isEmpty()) {
            item.setEncryptedPassword(crypto.encrypt(password, item.getId()));
        }
        item.setUpdatedAt(OffsetDateTime.now());
        item = itemRepository.save(item);
        auditService.record(adminEmail, AuditAction.UPDATE_ITEM, item.getTitle(), teamName(item), ip);
        return item;
    }

    @Transactional
    public void deleteItem(Long itemId, String adminEmail, String ip) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found"));
        String title = item.getTitle();
        String team = teamName(item);
        itemRepository.delete(item);
        auditService.record(adminEmail, AuditAction.DELETE_ITEM, title, team, ip);
    }

    /** Membership is enforced by the repository query; non-members simply see nothing. */
    @Transactional(readOnly = true)
    public List<Item> listTeamItems(Long teamId, Long userId) {
        return itemRepository.findAllByTeamIdForUser(teamId, userId);
    }

    /** Decrypts the secret for a member and records who looked at what. */
    @Transactional
    public String revealSecret(Long itemId, Long userId, String userEmail, String ip) {
        Item item = itemRepository.findByIdForUser(itemId, userId)
                .orElseThrow(() -> new NotFoundException("Item not found"));
        String plaintext = crypto.decrypt(item.getEncryptedPassword(), item.getId());
        auditService.record(userEmail, AuditAction.VIEW_SECRET, item.getTitle(), teamName(item), ip);
        return plaintext;
    }

    private String teamName(Item item) {
        if (item.getTeamId() == null) {
            return null;
        }
        return teamRepository.findById(item.getTeamId()).map(Team::getName).orElse(null);
    }
}
