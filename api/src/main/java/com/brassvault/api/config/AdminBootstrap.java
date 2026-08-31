package com.brassvault.api.config;

import com.brassvault.api.domain.Role;
import com.brassvault.api.domain.User;
import com.brassvault.api.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the very first admin on an empty database so the system is usable.
 * The initial password comes from the environment and must be rotated on
 * first login (must_change_password). Nothing is seeded when users exist
 * or when no initial password is configured.
 */
@Component
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminInitialPassword;

    public AdminBootstrap(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${admin.email:admin@brassvault.local}") String adminEmail,
                          @Value("${admin.initial-password:}") String adminInitialPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminInitialPassword = adminInitialPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        if (adminInitialPassword == null || adminInitialPassword.isBlank()) {
            log.warn("User table is empty and ADMIN_INITIAL_PASSWORD is not set; "
                    + "no admin was seeded. Set it and restart to bootstrap.");
            return;
        }
        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setFullName("Bootstrap Admin");
        admin.setRole(Role.ADMIN);
        admin.setPasswordHash(passwordEncoder.encode(adminInitialPassword));
        admin.setMustChangePassword(true);
        userRepository.save(admin);
        log.info("Seeded bootstrap admin '{}'; password change is forced on first login.", adminEmail);
    }
}
