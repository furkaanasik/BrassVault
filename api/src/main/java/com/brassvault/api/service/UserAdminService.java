package com.brassvault.api.service;

import com.brassvault.api.domain.Role;
import com.brassvault.api.domain.User;
import com.brassvault.api.repo.UserRepository;
import com.brassvault.api.web.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserAdminService {

    public record CreatedUser(User user, String tempPassword) {
    }

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TempPasswordGenerator tempPasswordGenerator;

    public UserAdminService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                            TempPasswordGenerator tempPasswordGenerator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tempPasswordGenerator = tempPasswordGenerator;
    }

    /** The temporary password lives only in the response; only its hash is stored. */
    @Transactional
    public CreatedUser createUser(String email, String fullName, Role role) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("A user with this email already exists");
        }
        String tempPassword = tempPasswordGenerator.generate();
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setMustChangePassword(true);
        return new CreatedUser(userRepository.save(user), tempPassword);
    }

    public List<User> listUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User updateUser(Long id, Role role, Boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (role != null) {
            user.setRole(role);
        }
        if (active != null) {
            user.setActive(active);
        }
        return userRepository.save(user);
    }
}
