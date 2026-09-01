package com.brassvault.api.web;

import com.brassvault.api.IntegrationTestBase;
import com.brassvault.api.crypto.VaultCryptoException;
import com.brassvault.api.crypto.VaultCryptoService;
import com.brassvault.api.domain.Role;
import com.brassvault.api.service.ItemService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ItemTypeCustomFieldsIT extends IntegrationTestBase {

    private static final String SECRET = "prim4ry-s3cret!";
    private static final String FIELD_VALUE_1 = "AKIA123";
    private static final String FIELD_VALUE_2 = "s3cret-value";

    @Autowired private ItemService itemService;
    @Autowired private VaultCryptoService crypto;

    private Cookie ownerCookie;
    private Cookie otherCookie;
    private long ownerId;

    @BeforeEach
    void setUpUsers() throws Exception {
        ownerId = createUser("owner@brassvault.local", Role.USER, false).getId();
        createUser("other@brassvault.local", Role.USER, false);
        ownerCookie = loginCookie("owner@brassvault.local");
        otherCookie = loginCookie("other@brassvault.local");
    }

    private long createApiKeyItem(String title) throws Exception {
        var result = mockMvc.perform(post("/api/vault/items").cookie(ownerCookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title,
                                "password", SECRET,
                                "type", "API_KEY",
                                "customFields", Map.of(
                                        "api_key_id", FIELD_VALUE_1,
                                        "endpoint_secret", FIELD_VALUE_2)))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    @DisplayName("create with type + custom fields stores an encrypted blob without plaintext values")
    void createWithTypeAndFieldsEncrypts() throws Exception {
        long itemId = createApiKeyItem("Cloud API");
        var item = itemRepository.findById(itemId).orElseThrow();
        assertThat(item.getType().name()).isEqualTo("API_KEY");
        byte[] blob = item.getEncryptedFields();
        assertThat(blob).isNotNull();
        String raw = new String(blob);
        assertThat(raw).doesNotContain(FIELD_VALUE_1).doesNotContain(FIELD_VALUE_2)
                .doesNotContain("api_key_id");
    }

    @Test
    @DisplayName("type defaults to LOGIN when omitted")
    void typeDefaultsToLogin() throws Exception {
        mockMvc.perform(post("/api/vault/items").cookie(ownerCookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Plain", "password", SECRET))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("LOGIN"));
    }

    @Test
    @DisplayName("list carries type but never custom field keys or values")
    void listCarriesTypeButNeverFields() throws Exception {
        createApiKeyItem("Cloud API");
        var result = mockMvc.perform(get("/api/vault/items").cookie(ownerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("API_KEY"))
                .andReturn();
        String json = result.getResponse().getContentAsString();
        assertThat(json).doesNotContain("customFields")
                .doesNotContain(FIELD_VALUE_1)
                .doesNotContain(FIELD_VALUE_2)
                .doesNotContain("api_key_id");
    }

    @Test
    @DisplayName("reveal returns password + fields and writes a single VIEW_SECRET audit entry")
    void revealReturnsFields() throws Exception {
        long itemId = createApiKeyItem("Cloud API");
        mockMvc.perform(get("/api/items/" + itemId + "/secret").cookie(ownerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").value(SECRET))
                .andExpect(jsonPath("$.fields.api_key_id").value(FIELD_VALUE_1))
                .andExpect(jsonPath("$.fields.endpoint_secret").value(FIELD_VALUE_2));
        assertThat(auditLogRepository.findAll())
                .filteredOn(a -> a.getAction().name().equals("VIEW_SECRET"))
                .hasSize(1);
    }

    @Test
    @DisplayName("update: provided map replaces, null keeps, empty map clears")
    void updateReplacesFieldsWhenProvided_keepsWhenNull() throws Exception {
        long itemId = createApiKeyItem("Cloud API");

        // full replace
        mockMvc.perform(put("/api/vault/items/" + itemId).cookie(ownerCookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Cloud API", "type", "API_KEY",
                                "customFields", Map.of("only_key", "only-value")))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/items/" + itemId + "/secret").cookie(ownerCookie))
                .andExpect(jsonPath("$.fields.only_key").value("only-value"))
                .andExpect(jsonPath("$.fields.api_key_id").doesNotExist());

        // null (absent) keeps
        mockMvc.perform(put("/api/vault/items/" + itemId).cookie(ownerCookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Cloud API", "type", "API_KEY"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/items/" + itemId + "/secret").cookie(ownerCookie))
                .andExpect(jsonPath("$.fields.only_key").value("only-value"));

        // empty map clears
        mockMvc.perform(put("/api/vault/items/" + itemId).cookie(ownerCookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Cloud API", "type", "API_KEY",
                                "customFields", Map.of()))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/items/" + itemId + "/secret").cookie(ownerCookie))
                .andExpect(jsonPath("$.fields").isEmpty());
        assertThat(itemRepository.findById(itemId).orElseThrow().getEncryptedFields()).isNull();
    }

    @Test
    @DisplayName("validation rejects too many fields, oversized keys and unknown types with 400")
    void validationRejects() throws Exception {
        Map<String, String> tooMany = new HashMap<>();
        for (int i = 0; i < 21; i++) {
            tooMany.put("key_" + i, "v");
        }
        mockMvc.perform(post("/api/vault/items").cookie(ownerCookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Too many", "password", SECRET,
                                "customFields", tooMany))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/vault/items").cookie(ownerCookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Long key", "password", SECRET,
                                "customFields", Map.of("k".repeat(65), "v")))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/vault/items").cookie(ownerCookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Bad type", "password", SECRET,
                                "type", "NOT_A_TYPE"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("fields blob is bound to its item and its :fields context — swapping fails to decrypt")
    void fieldsBlobBoundToItemAndContext() throws Exception {
        long itemAId = createApiKeyItem("Item A");
        long itemBId = createApiKeyItem("Item B");

        byte[] fieldsBlobA = itemRepository.findById(itemAId).orElseThrow().getEncryptedFields();

        // cross-item swap: A's fields blob on item B must not decrypt
        var itemB = itemRepository.findById(itemBId).orElseThrow();
        itemB.setEncryptedFields(fieldsBlobA);
        itemRepository.save(itemB);
        assertThatThrownBy(() -> itemService.revealSecret(itemBId, ownerId, "owner@brassvault.local", "127.0.0.1"))
                .isInstanceOf(VaultCryptoException.class);

        // cross-context swap: fields blob without the ":fields" context must not decrypt
        assertThatThrownBy(() -> crypto.decrypt(fieldsBlobA, itemAId))
                .isInstanceOf(VaultCryptoException.class);
    }

    @Test
    @DisplayName("typed personal item with fields is still invisible to other users — 404")
    void personalPrivacyUnchanged() throws Exception {
        long itemId = createApiKeyItem("Cloud API");
        mockMvc.perform(get("/api/items/" + itemId + "/secret").cookie(otherCookie))
                .andExpect(status().isNotFound());
    }
}
