package com.brassvault.api.web;

import com.brassvault.api.IntegrationTestBase;
import com.brassvault.api.domain.Role;
import com.brassvault.api.domain.User;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PersonalVaultFlowIT extends IntegrationTestBase {

    private static final String SECRET = "my-pers0nal-s3cret!";

    private Cookie ownerCookie;
    private Cookie otherCookie;
    private Cookie adminCookie;
    private User owner;

    @BeforeEach
    void setUpVault() throws Exception {
        owner = createUser("owner@brassvault.local", Role.USER, false);
        createUser("other@brassvault.local", Role.USER, false);
        createUser("admin@brassvault.local", Role.ADMIN, false);

        ownerCookie = loginCookie("owner@brassvault.local");
        otherCookie = loginCookie("other@brassvault.local");
        adminCookie = loginCookie("admin@brassvault.local");
    }

    private long createPersonalItem(Cookie cookie, String title) throws Exception {
        var result = mockMvc.perform(post("/api/vault/items").cookie(cookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title, "username", "me",
                                "password", SECRET,
                                "url", "https://private.example", "notes", "mine only"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    @DisplayName("create stores an AES-GCM blob (IV||ct||tag), never plaintext, and audits CREATE_ITEM with null team")
    void createEncryptsAndAudits() throws Exception {
        long itemId = createPersonalItem(ownerCookie, "My Bank");
        var item = itemRepository.findById(itemId).orElseThrow();
        assertThat(item.getOwnerId()).isEqualTo(owner.getId());
        assertThat(item.getTeamId()).isNull();
        byte[] blob = item.getEncryptedPassword();
        assertThat(blob).hasSize(12 + SECRET.getBytes().length + 16);
        assertThat(new String(blob)).doesNotContain(SECRET);
        assertThat(auditLogRepository.findAll())
                .anyMatch(a -> a.getAction().name().equals("CREATE_ITEM")
                        && "My Bank".equals(a.getItemTitle())
                        && a.getTeamName() == null);
    }

    @Test
    @DisplayName("list is scoped to the owner and carries no password field")
    void listScopedToOwner() throws Exception {
        createPersonalItem(ownerCookie, "Owner Item");
        createPersonalItem(otherCookie, "Other Item");

        var result = mockMvc.perform(get("/api/vault/items").cookie(ownerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Owner Item"))
                .andExpect(jsonPath("$[0].teamId").isEmpty())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        assertThat(json).doesNotContain("password").doesNotContain(SECRET);

        mockMvc.perform(get("/api/vault/items").cookie(otherCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Other Item"));
    }

    @Test
    @DisplayName("owner reveals the secret; plaintext matches and VIEW_SECRET is audited")
    void ownerRevealsSecret() throws Exception {
        long itemId = createPersonalItem(ownerCookie, "My Bank");
        mockMvc.perform(get("/api/items/" + itemId + "/secret").cookie(ownerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").value(SECRET));
        assertThat(auditLogRepository.findAll())
                .anyMatch(a -> a.getAction().name().equals("VIEW_SECRET")
                        && "owner@brassvault.local".equals(a.getUserEmail())
                        && "My Bank".equals(a.getItemTitle())
                        && a.getTeamName() == null);
    }

    @Test
    @DisplayName("another user asking for the secret gets 404, not 403 — existence is not leaked")
    void otherUserGets404() throws Exception {
        long itemId = createPersonalItem(ownerCookie, "My Bank");
        mockMvc.perform(get("/api/items/" + itemId + "/secret").cookie(otherCookie))
                .andExpect(status().isNotFound());
        assertThat(auditLogRepository.findAll())
                .noneMatch(a -> a.getAction().name().equals("VIEW_SECRET"));
    }

    @Test
    @DisplayName("admin cannot reveal, update or delete a personal item — all 404")
    void adminIsLockedOut() throws Exception {
        long itemId = createPersonalItem(ownerCookie, "My Bank");

        mockMvc.perform(get("/api/items/" + itemId + "/secret").cookie(adminCookie))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/admin/items/" + itemId).cookie(adminCookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Hacked", "password", "stolen"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/admin/items/" + itemId).cookie(adminCookie))
                .andExpect(status().isNotFound());

        var item = itemRepository.findById(itemId).orElseThrow();
        assertThat(item.getTitle()).isEqualTo("My Bank");
    }

    @Test
    @DisplayName("another user cannot update or delete a foreign personal item via the vault API — 404")
    void otherUserCannotMutate() throws Exception {
        long itemId = createPersonalItem(ownerCookie, "My Bank");

        mockMvc.perform(put("/api/vault/items/" + itemId).cookie(otherCookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Hijacked", "password", "x"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/vault/items/" + itemId).cookie(otherCookie))
                .andExpect(status().isNotFound());

        assertThat(itemRepository.findById(itemId)).isPresent();
    }

    @Test
    @DisplayName("update with a new password re-encrypts the blob; without one keeps it")
    void updateReencrypts() throws Exception {
        long itemId = createPersonalItem(ownerCookie, "My Bank");
        byte[] before = itemRepository.findById(itemId).orElseThrow().getEncryptedPassword();

        mockMvc.perform(put("/api/vault/items/" + itemId).cookie(ownerCookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "My Bank v2", "username", "me2",
                                "password", "new-Secret-42!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("My Bank v2"));

        byte[] after = itemRepository.findById(itemId).orElseThrow().getEncryptedPassword();
        assertThat(after).isNotEqualTo(before);
        mockMvc.perform(get("/api/items/" + itemId + "/secret").cookie(ownerCookie))
                .andExpect(jsonPath("$.password").value("new-Secret-42!"));

        // update without password keeps the old secret
        mockMvc.perform(put("/api/vault/items/" + itemId).cookie(ownerCookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "My Bank v3", "username", "me2"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/items/" + itemId + "/secret").cookie(ownerCookie))
                .andExpect(jsonPath("$.password").value("new-Secret-42!"));
        assertThat(auditLogRepository.findAll())
                .anyMatch(a -> a.getAction().name().equals("UPDATE_ITEM")
                        && a.getTeamName() == null);
    }

    @Test
    @DisplayName("delete removes the item and audits DELETE_ITEM with null team")
    void deleteAudits() throws Exception {
        long itemId = createPersonalItem(ownerCookie, "My Bank");
        mockMvc.perform(delete("/api/vault/items/" + itemId).cookie(ownerCookie))
                .andExpect(status().isNoContent());
        assertThat(itemRepository.findById(itemId)).isEmpty();
        assertThat(auditLogRepository.findAll())
                .anyMatch(a -> a.getAction().name().equals("DELETE_ITEM")
                        && "My Bank".equals(a.getItemTitle())
                        && a.getTeamName() == null);
    }

    @Test
    @DisplayName("creating a personal item with a blank title yields 400")
    void blankTitleRejected() throws Exception {
        mockMvc.perform(post("/api/vault/items").cookie(ownerCookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "", "password", "p"))))
                .andExpect(status().isBadRequest());
    }
}
