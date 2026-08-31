package com.brassvault.api.config;

import com.brassvault.api.IntegrationTestBase;
import com.brassvault.api.domain.Role;
import com.brassvault.api.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminBootstrapIT extends IntegrationTestBase {

    @Test
    @DisplayName("empty database gets a seeded admin with forced password change")
    void seedsAdminOnEmptyDatabase() {
        new AdminBootstrap(userRepository, passwordEncoder,
                "root@brassvault.local", "bootstrap-secret-123").run();
        User admin = userRepository.findByEmail("root@brassvault.local").orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.isMustChangePassword()).isTrue();
        assertThat(admin.getPasswordHash()).startsWith("$2");
    }

    @Test
    @DisplayName("no seeding when users already exist")
    void skipsWhenUsersExist() {
        createUser("existing@brassvault.local", Role.USER, false);
        new AdminBootstrap(userRepository, passwordEncoder,
                "root@brassvault.local", "bootstrap-secret-123").run();
        assertThat(userRepository.findByEmail("root@brassvault.local")).isEmpty();
    }

    @Test
    @DisplayName("no seeding without an initial password")
    void skipsWithoutPassword() {
        new AdminBootstrap(userRepository, passwordEncoder,
                "root@brassvault.local", "").run();
        assertThat(userRepository.count()).isZero();
    }
}
