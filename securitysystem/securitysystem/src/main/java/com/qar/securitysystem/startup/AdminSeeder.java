package com.qar.securitysystem.startup;

import com.qar.securitysystem.config.AdminProperties;
import com.qar.securitysystem.model.UserEntity;
import com.qar.securitysystem.model.UserRole;
import com.qar.securitysystem.repo.UserRepository;
import com.qar.securitysystem.util.IdUtil;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AdminSeeder implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder, AdminProperties adminProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String adminUsername = adminProperties.getUsername();
        String adminPassword = adminProperties.getPassword();
        if (adminUsername == null || adminUsername.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            return;
        }

        UserEntity existing = userRepository.findByAccount(adminUsername).orElse(null);
        if (existing == null) {
            UserEntity u = new UserEntity();
            u.setId(IdUtil.newId());
            u.setAccount(adminUsername);
            u.setPasswordHash(passwordEncoder.encode(adminPassword));
            u.setRole(UserRole.ADMIN);
            u.setCreatedAt(Instant.now());
            userRepository.save(u);
            return;
        }

        if (existing.getPasswordHash() == null || !passwordEncoder.matches(adminPassword, existing.getPasswordHash())) {
            existing.setPasswordHash(passwordEncoder.encode(adminPassword));
            userRepository.save(existing);
        }

        if (existing.getRole() != UserRole.ADMIN) {
            existing.setRole(UserRole.ADMIN);
            userRepository.save(existing);
        }
    }
}
