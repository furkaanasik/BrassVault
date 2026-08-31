package com.brassvault.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.brassvault.api.domain.Role;
import com.brassvault.api.domain.User;
import com.brassvault.api.repo.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "vault.master-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "jwt.secret=integration-test-signing-secret-with-plenty-of-entropy-0123456789"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestBase {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected UserRepository userRepository;
    @Autowired protected TeamRepository teamRepository;
    @Autowired protected TeamMemberRepository teamMemberRepository;
    @Autowired protected ItemRepository itemRepository;
    @Autowired protected AuditLogRepository auditLogRepository;
    @Autowired protected PasswordEncoder passwordEncoder;

    public static final String DEFAULT_PASSWORD = "correct-horse-battery";

    @BeforeEach
    void cleanDatabase() {
        auditLogRepository.deleteAll();
        itemRepository.deleteAll();
        teamMemberRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected User createUser(String email, Role role, boolean mustChangePassword) {
        User user = new User();
        user.setEmail(email);
        user.setFullName("Test " + email);
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setMustChangePassword(mustChangePassword);
        return userRepository.save(user);
    }

    protected Cookie loginCookie(String email) throws Exception {
        return loginCookie(email, DEFAULT_PASSWORD);
    }

    protected Cookie loginCookie(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        jakarta.servlet.http.Cookie cookie = result.getResponse().getCookie("ACCESS_TOKEN");
        return Objects.requireNonNull(cookie, "login did not set ACCESS_TOKEN cookie");
    }
}
