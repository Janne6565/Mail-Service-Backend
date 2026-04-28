package com.janne.mailservice.configuration.admin;

import com.janne.mailservice.entity.Role;
import com.janne.mailservice.entity.UserEntity;
import com.janne.mailservice.repository.UserRepository;
import com.janne.mailservice.services.core.UserService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final AdminProperties adminProperties;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        Optional<UserEntity> existingAdmin =
                userRepository.findAll().stream()
                        .filter(u -> u.getRole() == Role.ADMIN)
                        .findFirst();

        if (existingAdmin.isEmpty()) {
            userService.createAdminUser(
                    adminProperties.getUsername(),
                    adminProperties.getEmail(),
                    adminProperties.getPassword());
            log.info("Admin user '{}' created", adminProperties.getUsername());
        } else {
            UserEntity admin = existingAdmin.get();
            boolean changed = false;
            if (!admin.getEmail().equalsIgnoreCase(adminProperties.getEmail())) {
                admin.setEmail(adminProperties.getEmail());
                changed = true;
            }
            if (!passwordEncoder.matches(adminProperties.getPassword(), admin.getPasswordHash())) {
                admin.setPasswordHash(passwordEncoder.encode(adminProperties.getPassword()));
                changed = true;
            }
            if (changed) {
                userRepository.save(admin);
                log.info("Admin user '{}' updated from env configuration", admin.getUsername());
            }
        }
    }
}
