package com.brassvault.api.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.brassvault.api.IntegrationTestBase;
import com.brassvault.api.domain.Role;
import com.brassvault.api.domain.Team;
import com.brassvault.api.domain.TeamMember;
import com.brassvault.api.domain.TeamMemberId;
import com.brassvault.api.domain.User;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ItemFlowIT extends IntegrationTestBase {

    private static final String SECRET = "pr0d-DB-p@ssw0rd!";

    private Cookie adminCookie;
    private Cookie memberCookie;
    private Cookie outsiderCookie;
    private User member;
    private Team team;

    @BeforeEach
    void setUpVault() throws Exception {
        createUser("admin@brassvault.local", Role.ADMIN, false);
        member = createUser("member@brassvault.local", Role.USER, false);
        createUser("outsider@brassvault.local", Role.USER, false);

        team = new Team();
        team.setName("Marco Polo");
        team = teamRepository.save(team);
        teamMemberRepository.save(new TeamMember(team.getId(), member.getId()));

        adminCookie = loginCookie("admin@brassvault.local");
        memberCookie = loginCookie("member@brassvault.local");
        outsiderCookie = loginCookie("outsider@brassvault.local");
    }

    private long createItem() throws Exception {
        var result = mockMvc.perform(post("/api/admin/items").cookie(adminCookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "teamId", team.getId(), "title", "Prod DB",
                                "username", "dbuser", "password", SECRET,
                                "url", "jdbc:postgresql://prod/db", "notes", "handle with care"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    @DisplayName("create stores an AES-GCM blob (IV||ct||tag), never plaintext, and audits CREATE_ITEM")
    void createEncryptsAndAudits() throws Exception {
        long itemId = createItem();
        var item = itemRepository.findById(itemId).orElseThrow();
        byte[] blob = item.getEncryptedPassword();
        assertThat(blob).hasSize(12 + SECRET.getBytes().length + 16);
        assertThat(new String(blob)).doesNotContain(SECRET);
        assertThat(auditLogRepository.findAll())
                .anyMatch(a -> a.getAction().name().equals("CREATE_ITEM")
                        && "Prod DB".equals(a.getItemTitle())
                        && "Marco Polo".equals(a.getTeamName()));
    }

    @Test
    @DisplayName("item list for a member carries no password field")
    void listHasNoPassword() throws Exception {
        createItem();
        var result = mockMvc.perform(get("/api/teams/" + team.getId() + "/items").cookie(memberCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Prod DB"))
                .andReturn();
        String json = result.getResponse().getContentAsString();
        assertThat(json).doesNotContain("password").doesNotContain(SECRET);
    }

    @Test
    @DisplayName("outsider sees an empty list for a foreign team")
    void outsiderListEmpty() throws Exception {
        createItem();
        mockMvc.perform(get("/api/teams/" + team.getId() + "/items").cookie(outsiderCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("member reveals the secret; plaintext matches and VIEW_SECRET is audited")
    void revealSecret() throws Exception {
        long itemId = createItem();
        mockMvc.perform(get("/api/items/" + itemId + "/secret").cookie(memberCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").value(SECRET));
        assertThat(auditLogRepository.findAll())
                .anyMatch(a -> a.getAction().name().equals("VIEW_SECRET")
                        && "member@brassvault.local".equals(a.getUserEmail())
                        && "Prod DB".equals(a.getItemTitle()));
    }

    @Test
    @DisplayName("outsider asking for the secret gets 404, not 403 — existence is not leaked")
    void outsiderGets404() throws Exception {
        long itemId = createItem();
        mockMvc.perform(get("/api/items/" + itemId + "/secret").cookie(outsiderCookie))
                .andExpect(status().isNotFound());
        assertThat(auditLogRepository.findAll())
                .noneMatch(a -> a.getAction().name().equals("VIEW_SECRET"));
    }

    @Test
    @DisplayName("member removed from the team loses access on the very next request")
    void removedMemberLosesAccess() throws Exception {
        long itemId = createItem();
        mockMvc.perform(get("/api/items/" + itemId + "/secret").cookie(memberCookie))
                .andExpect(status().isOk());

        teamMemberRepository.deleteById(new TeamMemberId(team.getId(), member.getId()));

        mockMvc.perform(get("/api/items/" + itemId + "/secret").cookie(memberCookie))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/teams/" + team.getId() + "/items").cookie(memberCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("update with a new password re-encrypts the blob; without one keeps it")
    void updateReencrypts() throws Exception {
        long itemId = createItem();
        byte[] before = itemRepository.findById(itemId).orElseThrow().getEncryptedPassword();

        mockMvc.perform(put("/api/admin/items/" + itemId).cookie(adminCookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Prod DB v2", "username", "dbuser2",
                                "password", "new-Secret-42!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Prod DB v2"));

        byte[] after = itemRepository.findById(itemId).orElseThrow().getEncryptedPassword();
        assertThat(after).isNotEqualTo(before);
        mockMvc.perform(get("/api/items/" + itemId + "/secret").cookie(memberCookie))
                .andExpect(jsonPath("$.password").value("new-Secret-42!"));

        // update without password keeps the old secret
        mockMvc.perform(put("/api/admin/items/" + itemId).cookie(adminCookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Prod DB v3", "username", "dbuser2"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/items/" + itemId + "/secret").cookie(memberCookie))
                .andExpect(jsonPath("$.password").value("new-Secret-42!"));
        assertThat(auditLogRepository.findAll())
                .anyMatch(a -> a.getAction().name().equals("UPDATE_ITEM"));
    }

    @Test
    @DisplayName("delete removes the item and audits DELETE_ITEM with snapshots")
    void deleteAudits() throws Exception {
        long itemId = createItem();
        mockMvc.perform(delete("/api/admin/items/" + itemId).cookie(adminCookie))
                .andExpect(status().isNoContent());
        assertThat(itemRepository.findById(itemId)).isEmpty();
        assertThat(auditLogRepository.findAll())
                .anyMatch(a -> a.getAction().name().equals("DELETE_ITEM")
                        && "Prod DB".equals(a.getItemTitle())
                        && "Marco Polo".equals(a.getTeamName()));
    }

    @Test
    @DisplayName("creating an item for a missing team yields 404")
    void createForMissingTeam() throws Exception {
        mockMvc.perform(post("/api/admin/items").cookie(adminCookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "teamId", 99999, "title", "X", "password", "p"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("audit log endpoint filters by action and email, and paginates")
    void auditLogFiltering() throws Exception {
        long itemId = createItem();
        mockMvc.perform(get("/api/items/" + itemId + "/secret").cookie(memberCookie))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/audit-logs").cookie(adminCookie)
                        .param("action", "VIEW_SECRET"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("VIEW_SECRET"))
                .andExpect(jsonPath("$.content[0].userEmail").value("member@brassvault.local"));

        mockMvc.perform(get("/api/admin/audit-logs").cookie(adminCookie)
                        .param("userEmail", "nobody@brassvault.local"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        mockMvc.perform(get("/api/admin/audit-logs").cookie(adminCookie)
                        .param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }
}
