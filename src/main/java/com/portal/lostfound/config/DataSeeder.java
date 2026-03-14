package com.portal.lostfound.config;

import com.portal.lostfound.model.User;
import com.portal.lostfound.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedAdmin();
        seedTestUser();
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail("admin@lostfound.com")) {
            log.info("Admin already exists — skipping seed");
            return;
        }
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@lostfound.com");
        admin.setPassword(passwordEncoder.encode("Admin1234"));
        admin.setFullName("System Admin");
        admin.setPhoneNumber("0000000000");
        admin.setRole(User.Role.ADMIN);
        admin.setActive(true);
        userRepository.save(admin);
        log.info("✅ Admin seeded  →  admin@lostfound.com / Admin1234");
    }

    private void seedTestUser() {
        if (userRepository.existsByEmail("test@lostfound.com")) {
            log.info("Test user already exists — skipping seed");
            return;
        }
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@lostfound.com");
        user.setPassword(passwordEncoder.encode("Test1234"));
        user.setFullName("Test User");
        user.setPhoneNumber("9999999999");
        user.setRole(User.Role.USER);
        user.setActive(true);
        userRepository.save(user);
        log.info("✅ Test user seeded  →  test@lostfound.com / Test1234");
    }
}
