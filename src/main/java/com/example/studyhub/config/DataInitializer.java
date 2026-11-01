package com.example.studyhub.config;

import com.example.studyhub.entity.Role;
import com.example.studyhub.entity.User;
import com.example.studyhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            initSampleUsers();
        } catch (Exception ex) {
            log.warn("Không thể khởi tạo hoặc cập nhật dữ liệu mẫu (CSDL có thể chưa sẵn sàng): {}", ex.getMessage());
        }
    }

    private void initSampleUsers() {
        String defaultPasswordHash = passwordEncoder.encode("123456");

        // 1. Repair users with placeholder '<BCRYPT_HASH>' or invalid hash
        List<User> existingUsers = userRepository.findAll();
        for (User user : existingUsers) {
            if (user.getPasswordHash() == null || !user.getPasswordHash().startsWith("$2")) {
                user.setPasswordHash(defaultPasswordHash);
                userRepository.save(user);
                log.info("Đã cập nhật mật khẩu BCrypt mẫu (123456) cho tài khoản: {}", user.getUsername());
            }
        }

        // 2. If table is empty, seed initial sample accounts
        if (userRepository.count() == 0) {
            userRepository.save(User.builder()
                    .username("admin")
                    .passwordHash(defaultPasswordHash)
                    .fullName("Quản trị viên")
                    .email("admin@school.edu.vn")
                    .role(Role.ADMIN)
                    .isActive(true)
                    .build());

            userRepository.save(User.builder()
                    .username("gv001")
                    .passwordHash(defaultPasswordHash)
                    .fullName("Nguyễn Văn An")
                    .email("gv001@school.edu.vn")
                    .role(Role.LECTURER)
                    .isActive(true)
                    .build());

            userRepository.save(User.builder()
                    .username("sv001")
                    .passwordHash(defaultPasswordHash)
                    .fullName("Trần Thị Bình")
                    .email("sv001@school.edu.vn")
                    .role(Role.STUDENT)
                    .isActive(true)
                    .build());

            userRepository.save(User.builder()
                    .username("sv002")
                    .passwordHash(defaultPasswordHash)
                    .fullName("Lê Văn Cường")
                    .email("sv002@school.edu.vn")
                    .role(Role.STUDENT)
                    .isActive(true)
                    .build());

            log.info("Đã tạo 4 tài khoản người dùng mẫu với mật khẩu mặc định '123456'.");
        }
    }
}
