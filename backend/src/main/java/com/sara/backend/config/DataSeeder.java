package com.sara.backend.config;

import com.sara.backend.entity.Role;
import com.sara.backend.entity.User;
import com.sara.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${dor.seed.admin-password}")
    private String adminPassword;

    @Value("${dor.seed.staff-password}")
    private String staffPassword;

    @Override
    public void run(String... args) {
        createUserIfMissing("Office Admin", "admin@dor.com", adminPassword, Role.ADMIN);
        createUserIfMissing("Desk Staff", "staff@dor.com", staffPassword, Role.STAFF);
    }

    private void createUserIfMissing(String fullName, String email, String password, Role role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        userRepository.save(user);
    }
}