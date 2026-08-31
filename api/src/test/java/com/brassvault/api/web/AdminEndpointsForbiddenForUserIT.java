package com.brassvault.api.web;

import com.brassvault.api.IntegrationTestBase;
import com.brassvault.api.domain.Role;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Every /api/admin/** endpoint must return 403 for a plain USER — one test per endpoint. */
class AdminEndpointsForbiddenForUserIT extends IntegrationTestBase {

    private Cookie userCookie;

    @BeforeEach
    void setUpUser() throws Exception {
        createUser("plain@brassvault.local", Role.USER, false);
        userCookie = loginCookie("plain@brassvault.local");
    }

    @Test
    @DisplayName("POST /api/admin/users is forbidden for USER")
    void createUserForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/users").cookie(userCookie)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/users is forbidden for USER")
    void listUsersForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users").cookie(userCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/admin/users/{id} is forbidden for USER")
    void patchUserForbidden() throws Exception {
        mockMvc.perform(patch("/api/admin/users/1").cookie(userCookie)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/admin/teams is forbidden for USER")
    void createTeamForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/teams").cookie(userCookie)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/teams is forbidden for USER")
    void listTeamsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/teams").cookie(userCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/teams/{id}/members is forbidden for USER")
    void listMembersForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/teams/1/members").cookie(userCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/admin/teams/{id}/members is forbidden for USER")
    void addMemberForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/teams/1/members").cookie(userCookie)
                        .contentType("application/json").content("{\"userId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/admin/teams/{id}/members/{uid} is forbidden for USER")
    void removeMemberForbidden() throws Exception {
        mockMvc.perform(delete("/api/admin/teams/1/members/1").cookie(userCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/admin/items is forbidden for USER")
    void createItemForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/items").cookie(userCookie)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/admin/items/{id} is forbidden for USER")
    void updateItemForbidden() throws Exception {
        mockMvc.perform(put("/api/admin/items/1").cookie(userCookie)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/admin/items/{id} is forbidden for USER")
    void deleteItemForbidden() throws Exception {
        mockMvc.perform(delete("/api/admin/items/1").cookie(userCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/audit-logs is forbidden for USER")
    void auditLogsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs").cookie(userCookie))
                .andExpect(status().isForbidden());
    }
}
