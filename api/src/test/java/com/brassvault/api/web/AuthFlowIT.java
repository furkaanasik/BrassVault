package com.brassvault.api.web;

import com.brassvault.api.IntegrationTestBase;
import com.brassvault.api.domain.Role;
import com.brassvault.api.domain.User;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthFlowIT extends IntegrationTestBase {

    @Test
    @DisplayName("successful login sets a hardened httpOnly cookie and audits LOGIN")
    void loginSetsCookie() throws Exception {
        createUser("yusuf@brassvault.local", Role.USER, false);
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "yusuf@brassvault.local", "password", DEFAULT_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("yusuf@brassvault.local"))
                .andReturn();
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).contains("HttpOnly").contains("Secure").contains("SameSite=Strict");
        assertThat(auditLogRepository.findAll())
                .anyMatch(a -> a.getUserEmail().equals("yusuf@brassvault.local")
                        && a.getAction().name().equals("LOGIN"));
    }

    @Test
    @DisplayName("wrong password yields 401")
    void wrongPassword() throws Exception {
        createUser("yusuf@brassvault.local", Role.USER, false);
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "yusuf@brassvault.local", "password", "wrong-password!"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("deactivated user cannot log in")
    void deactivatedUserCannotLogin() throws Exception {
        User user = createUser("gone@brassvault.local", Role.USER, false);
        user.setActive(false);
        userRepository.save(user);
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "gone@brassvault.local", "password", DEFAULT_PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("unauthenticated request yields 401")
    void unauthenticated() throws Exception {
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("must_change_password user is locked out of every endpoint except password change")
    void mustChangePasswordLockout() throws Exception {
        createUser("fresh@brassvault.local", Role.USER, true);
        Cookie cookie = loginCookie("fresh@brassvault.local");

        mockMvc.perform(get("/api/me").cookie(cookie)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/teams").cookie(cookie)).andExpect(status().isForbidden());

        mockMvc.perform(post("/api/me/password").cookie(cookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", DEFAULT_PASSWORD,
                                "newPassword", "brand-new-secure-password"))))
                .andExpect(status().isNoContent());

        Cookie fresh = loginCookie("fresh@brassvault.local", "brand-new-secure-password");
        mockMvc.perform(get("/api/me").cookie(fresh)).andExpect(status().isOk());
        mockMvc.perform(get("/api/teams").cookie(fresh)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("password change with wrong current password fails, too-short new password rejected")
    void passwordChangeValidation() throws Exception {
        createUser("val@brassvault.local", Role.USER, false);
        Cookie cookie = loginCookie("val@brassvault.local");

        mockMvc.perform(post("/api/me/password").cookie(cookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "totally-wrong",
                                "newPassword", "brand-new-secure-password"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/me/password").cookie(cookie)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", DEFAULT_PASSWORD,
                                "newPassword", "short"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("logout clears the cookie")
    void logoutClearsCookie() throws Exception {
        var result = mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andReturn();
        assertThat(result.getResponse().getHeader("Set-Cookie")).contains("Max-Age=0");
    }
}
