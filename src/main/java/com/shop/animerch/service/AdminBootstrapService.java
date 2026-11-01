
package com.shop.animerch.service;

import com.shop.animerch.model.User;
import com.shop.animerch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBootstrapService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Transactional
    public void promoteToAdmin() {

        User user = userRepository.findByEmail(adminEmail)
                .orElse(null);

        if (user != null) {

            log.info("Admin email: [{}]", adminEmail);
            log.info("Admin ENV password length: {}", adminPassword.length());

            boolean passwordMatches = passwordEncoder.matches(
                    adminPassword,
                    user.getPasswordHash()
            );

            log.info("Admin password matches: {}", passwordMatches);

            if (passwordMatches) {
                user.promoteToAdmin();
                userRepository.save(user);

                log.info("Admin successfully initialized: {}", adminEmail);
            } else {
                throw new IllegalStateException(
                        "Admin credentials do not match existing user"
                );
            }

        } else {

            String hashedPassword = passwordEncoder.encode(adminPassword);

            User admin = User.createAdmin(
                    adminEmail,
                    hashedPassword
            );

            userRepository.save(admin);

            log.info("Admin account created: {}", adminEmail);
        }
    }

    @Bean
    public CommandLineRunner initAdmin(AdminBootstrapService service) {
        return args -> service.promoteToAdmin();
    }
}
