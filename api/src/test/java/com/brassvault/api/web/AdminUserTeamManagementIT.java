package com.brassvault.api.web;

import com.brassvault.api.IntegrationTestBase;
import com.brassvault.api.domain.Role;
import com.brassvault.api.domain.User;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminUserTeamManagementIT extends IntegrationTestBase {

    private Cookie adminCookie;

    @BeforeEach
    void setUpAdmin() throws Exception {
        createUser("admin@brassvault.local", Role.ADMIN, false);
        adminCookie = loginCookie("admin@brassvault.local");
    }

    @Test
    @DisplayName("admin creates a user; temp password returned once, only hash stored, login works")
    void createUserFlow() throws Exception {
        var result = mockMvc.perform(post("/api/admin/users").cookie(adminCookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "yusuf@brassvault.local", "fullName", "Yusuf Yeni", "role", "USER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tempPassword").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String tempPassword = body.get("tempPassword").asText();
        assertThat(tempPassword).hasSize(16);

        User stored = userRepository.findByEmail("yusuf@brassvault.local").orElseThrow();
        assertThat(stored.getPasswordHash()).doesNotContain(tempPassword).startsWith("$2");
        assertThat(stored.isMustChangePassword()).isTrue();

        // temp password actually works for login
        loginCookie("yusuf@brassvault.local", tempPassword);
    }

    @Test
    @DisplayName("duplicate email is rejected")
    void duplicateEmailRejected() throws Exception {
        createUser("dup@brassvault.local", Role.USER, false);
        mockMvc.perform(post("/api/admin/users").cookie(adminCookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "dup@brassvault.local", "fullName", "Dup", "role", "USER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("admin deactivates a user; that user can no longer log in")
    void deactivateUser() throws Exception {
        User victim = createUser("victim@brassvault.local", Role.USER, false);
        mockMvc.perform(patch("/api/admin/users/" + victim.getId()).cookie(adminCookie)
                        .contentType("application/json")
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "victim@brassvault.local", "password", DEFAULT_PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("team lifecycle: create, add member (audited), member sees team, remove member")
    void teamLifecycle() throws Exception {
        User yusuf = createUser("yusuf@brassvault.local", Role.USER, false);

        var teamResult = mockMvc.perform(post("/api/admin/teams").cookie(adminCookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Marco Polo", "description", "explorers"))))
                .andExpect(status().isCreated())
                .andReturn();
        long teamId = objectMapper.readTree(teamResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/admin/teams/" + teamId + "/members").cookie(adminCookie)
                        .contentType("application/json")
                        .content("{\"userId\":" + yusuf.getId() + "}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/teams/" + teamId + "/members").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("yusuf@brassvault.local"));

        assertThat(auditLogRepository.findAll())
                .anyMatch(a -> a.getAction().name().equals("ADD_MEMBER")
                        && "Marco Polo".equals(a.getTeamName())
                        && "admin@brassvault.local".equals(a.getUserEmail()));

        Cookie yusufCookie = loginCookie("yusuf@brassvault.local");
        mockMvc.perform(get("/api/teams").cookie(yusufCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Marco Polo"));
        mockMvc.perform(get("/api/me").cookie(yusufCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teams[0].name").value("Marco Polo"));

        mockMvc.perform(delete("/api/admin/teams/" + teamId + "/members/" + yusuf.getId())
                        .cookie(adminCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/teams").cookie(yusufCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/admin/teams/" + teamId + "/members").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        assertThat(auditLogRepository.findAll())
                .anyMatch(a -> a.getAction().name().equals("REMOVE_MEMBER"));
    }

    @Test
    @DisplayName("adding a member to a missing team yields 404")
    void addMemberMissingTeam() throws Exception {
        User yusuf = createUser("yusuf@brassvault.local", Role.USER, false);
        mockMvc.perform(post("/api/admin/teams/99999/members").cookie(adminCookie)
                        .contentType("application/json")
                        .content("{\"userId\":" + yusuf.getId() + "}"))
                .andExpect(status().isNotFound());
    }
}
