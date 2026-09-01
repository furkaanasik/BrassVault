package com.brassvault.api.service;

import com.brassvault.api.crypto.VaultCryptoException;
import com.brassvault.api.crypto.VaultCryptoService;
import com.brassvault.api.domain.AuditAction;
import com.brassvault.api.domain.Item;
import com.brassvault.api.domain.ItemType;
import com.brassvault.api.domain.Team;
import com.brassvault.api.repo.ItemRepository;
import com.brassvault.api.repo.TeamRepository;
import com.brassvault.api.web.NotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ItemService {

    public static final String FIELDS_AAD_CONTEXT = ":fields";
    private static final int MAX_CUSTOM_FIELDS = 20;
    private static final int MAX_FIELD_KEY_LENGTH = 64;
    private static final int MAX_FIELD_VALUE_LENGTH = 10_000;

    private final ItemRepository itemRepository;
    private final TeamRepository teamRepository;
    private final VaultCryptoService crypto;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public ItemService(ItemRepository itemRepository, TeamRepository teamRepository,
                       VaultCryptoService crypto, AuditService auditService,
                       ObjectMapper objectMapper) {
        this.itemRepository = itemRepository;
        this.teamRepository = teamRepository;
        this.crypto = crypto;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    /** Everything a caller supplies for an item; customFields == null means "not provided". */
    public record ItemInput(String title, String username, String password, String url,
                            String notes, ItemType type, Map<String, String> customFields) {
        public ItemType typeOrDefault() {
            return type == null ? ItemType.LOGIN : type;
        }
    }

    public record RevealedSecret(String password, Map<String, String> fields) {
    }

    /**
     * The row is inserted first so the generated id exists, then the password is
     * encrypted with that id as AAD and the row updated — all in one transaction.
     */
    @Transactional
    public Item createItem(Long teamId, ItemInput input, Long adminId, String adminEmail, String ip) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Team not found"));
        Item item = new Item();
        item.setTeamId(teamId);
        item.setTitle(input.title());
        item.setUsername(input.username());
        item.setUrl(input.url());
        item.setNotes(input.notes());
        item.setType(input.typeOrDefault());
        item.setCreatedBy(adminId);
        item.setEncryptedPassword(new byte[0]);
        item = itemRepository.saveAndFlush(item);
        item.setEncryptedPassword(crypto.encrypt(input.password(), item.getId()));
        item.setEncryptedFields(encryptFields(input.customFields(), item.getId()));
        item = itemRepository.save(item);
        auditService.record(adminEmail, AuditAction.CREATE_ITEM, input.title(), team.getName(), ip);
        return item;
    }

    @Transactional
    public Item updateItem(Long itemId, ItemInput input, String adminEmail, String ip) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found"));
        if (item.getOwnerId() != null) {
            throw new NotFoundException("Item not found");
        }
        applyUpdate(item, input);
        item = itemRepository.save(item);
        auditService.record(adminEmail, AuditAction.UPDATE_ITEM, item.getTitle(), teamName(item), ip);
        return item;
    }

    @Transactional
    public void deleteItem(Long itemId, String adminEmail, String ip) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found"));
        if (item.getOwnerId() != null) {
            throw new NotFoundException("Item not found");
        }
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
    public RevealedSecret revealSecret(Long itemId, Long userId, String userEmail, String ip) {
        Item item = itemRepository.findByIdForUser(itemId, userId)
                .orElseThrow(() -> new NotFoundException("Item not found"));
        RevealedSecret secret = new RevealedSecret(
                crypto.decrypt(item.getEncryptedPassword(), item.getId()),
                decryptFields(item));
        auditService.record(userEmail, AuditAction.VIEW_SECRET, item.getTitle(), teamName(item), ip);
        return secret;
    }

    /**
     * The row is inserted first so the generated id exists, then the password is
     * encrypted with that id as AAD and the row updated — all in one transaction.
     */
    @Transactional
    public Item createPersonalItem(ItemInput input, Long ownerId, String ownerEmail, String ip) {
        Item item = new Item();
        item.setOwnerId(ownerId);
        item.setTitle(input.title());
        item.setUsername(input.username());
        item.setUrl(input.url());
        item.setNotes(input.notes());
        item.setType(input.typeOrDefault());
        item.setCreatedBy(ownerId);
        item.setEncryptedPassword(new byte[0]);
        item = itemRepository.saveAndFlush(item);
        item.setEncryptedPassword(crypto.encrypt(input.password(), item.getId()));
        item.setEncryptedFields(encryptFields(input.customFields(), item.getId()));
        item = itemRepository.save(item);
        auditService.record(ownerEmail, AuditAction.CREATE_ITEM, input.title(), null, ip);
        return item;
    }

    @Transactional(readOnly = true)
    public List<Item> listPersonalItems(Long ownerId) {
        return itemRepository.findAllByOwnerId(ownerId);
    }

    @Transactional
    public Item updatePersonalItem(Long itemId, Long ownerId, ItemInput input,
                                   String ownerEmail, String ip) {
        Item item = itemRepository.findByIdAndOwnerId(itemId, ownerId)
                .orElseThrow(() -> new NotFoundException("Item not found"));
        applyUpdate(item, input);
        item = itemRepository.save(item);
        auditService.record(ownerEmail, AuditAction.UPDATE_ITEM, item.getTitle(), null, ip);
        return item;
    }

    @Transactional
    public void deletePersonalItem(Long itemId, Long ownerId, String ownerEmail, String ip) {
        Item item = itemRepository.findByIdAndOwnerId(itemId, ownerId)
                .orElseThrow(() -> new NotFoundException("Item not found"));
        String title = item.getTitle();
        itemRepository.delete(item);
        auditService.record(ownerEmail, AuditAction.DELETE_ITEM, title, null, ip);
    }

    private void applyUpdate(Item item, ItemInput input) {
        item.setTitle(input.title());
        item.setUsername(input.username());
        item.setUrl(input.url());
        item.setNotes(input.notes());
        item.setType(input.typeOrDefault());
        if (input.password() != null && !input.password().isEmpty()) {
            item.setEncryptedPassword(crypto.encrypt(input.password(), item.getId()));
        }
        // null = keep current fields; empty map = clear; non-empty = full replace
        if (input.customFields() != null) {
            item.setEncryptedFields(encryptFields(input.customFields(), item.getId()));
        }
        item.setUpdatedAt(OffsetDateTime.now());
    }

    private byte[] encryptFields(Map<String, String> fields, long itemId) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        validateCustomFields(fields);
        try {
            return crypto.encrypt(objectMapper.writeValueAsString(fields), itemId, FIELDS_AAD_CONTEXT);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Custom fields could not be serialized");
        }
    }

    private Map<String, String> decryptFields(Item item) {
        if (item.getEncryptedFields() == null) {
            return Map.of();
        }
        try {
            String json = crypto.decrypt(item.getEncryptedFields(), item.getId(), FIELDS_AAD_CONTEXT);
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (JsonProcessingException e) {
            throw new VaultCryptoException("Custom fields blob is corrupt");
        }
    }

    private static void validateCustomFields(Map<String, String> fields) {
        if (fields.size() > MAX_CUSTOM_FIELDS) {
            throw new IllegalArgumentException("At most " + MAX_CUSTOM_FIELDS + " custom fields");
        }
        fields.forEach((k, v) -> {
            if (k == null || k.isBlank() || k.length() > MAX_FIELD_KEY_LENGTH) {
                throw new IllegalArgumentException("Custom field key must be 1-" + MAX_FIELD_KEY_LENGTH + " chars");
            }
            if (v == null || v.length() > MAX_FIELD_VALUE_LENGTH) {
                throw new IllegalArgumentException("Custom field value too long");
            }
        });
    }

    private String teamName(Item item) {
        if (item.getTeamId() == null) {
            return null;
        }
        return teamRepository.findById(item.getTeamId()).map(Team::getName).orElse(null);
    }
}
