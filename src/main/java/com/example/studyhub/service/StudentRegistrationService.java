package com.example.studyhub.service;

import com.example.studyhub.dtos.StudentRegisterDto;
import com.example.studyhub.entity.Department;
import com.example.studyhub.entity.Role;
import com.example.studyhub.entity.Student;
import com.example.studyhub.entity.User;
import com.example.studyhub.repository.DepartmentRepository;
import com.example.studyhub.repository.StudentRepository;
import com.example.studyhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentRegistrationService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    @Transactional
    public User registerStudent(StudentRegisterDto dto) {
        validateRegistration(dto);

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("Khoa / Bộ môn được chọn không tồn tại!"));

        String cleanUsername = dto.getUsername().trim().toLowerCase();
        String cleanEmail = dto.getEmail().trim().toLowerCase();
        String cleanFullName = dto.getFullName().trim();
        String cleanPhone = (dto.getPhone() != null && !dto.getPhone().isBlank()) ? dto.getPhone().trim() : null;
        String cleanStudentCode = generateStudentCode();

        // 1. Create and save User
        User user = User.builder()
                .username(cleanUsername)
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .fullName(cleanFullName)
                .email(cleanEmail)
                .phone(cleanPhone)
                .role(Role.STUDENT)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        // 2. Create and save Student profile
        Student student = Student.builder()
                .userId(savedUser.getId())
                .user(savedUser)
                .studentCode(cleanStudentCode)
                .className(null)
                .department(department)
                .isNew(true)
                .build();

        studentRepository.save(student);

        log.info("Đã tạo mới tài khoản sinh viên: username={}, studentCode={}", cleanUsername, cleanStudentCode);
        return savedUser;
    }

    public synchronized String generateStudentCode() {
        long count = studentRepository.count();
        long nextNumber = count + 1;
        String candidate = String.format("SV%03d", nextNumber);
        while (studentRepository.existsByStudentCode(candidate)) {
            nextNumber++;
            candidate = String.format("SV%03d", nextNumber);
        }
        return candidate;
    }

    private void validateRegistration(StudentRegisterDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Dữ liệu đăng ký không hợp lệ!");
        }

        // Full name
        if (dto.getFullName() == null || dto.getFullName().trim().isBlank()) {
            throw new IllegalArgumentException("Họ và tên không được để trống!");
        }
        if (dto.getFullName().trim().length() > 150) {
            throw new IllegalArgumentException("Họ và tên không được vượt quá 150 ký tự!");
        }

        // Department
        if (dto.getDepartmentId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn Khoa / Bộ môn!");
        }

        // Username
        if (dto.getUsername() == null || dto.getUsername().trim().isBlank()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống!");
        }
        String cleanUsername = dto.getUsername().trim().toLowerCase();
        if (cleanUsername.length() < 3 || cleanUsername.length() > 50) {
            throw new IllegalArgumentException("Tên đăng nhập phải có từ 3 đến 50 ký tự!");
        }
        if (!cleanUsername.matches("^[a-zA-Z0-9_.-]+$")) {
            throw new IllegalArgumentException("Tên đăng nhập chỉ được chứa chữ cái, số, dấu gạch dưới, gạch ngang hoặc dấu chấm!");
        }
        if (userRepository.existsByUsername(cleanUsername)) {
            throw new IllegalArgumentException("Tên đăng nhập '" + cleanUsername + "' đã được sử dụng! Vui lòng chọn tên khác.");
        }

        // Email
        if (dto.getEmail() == null || dto.getEmail().trim().isBlank()) {
            throw new IllegalArgumentException("Email không được để trống!");
        }
        String cleanEmail = dto.getEmail().trim().toLowerCase();
        if (cleanEmail.length() > 150) {
            throw new IllegalArgumentException("Email không được vượt quá 150 ký tự!");
        }
        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            throw new IllegalArgumentException("Định dạng email không hợp lệ!");
        }
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("Email '" + cleanEmail + "' đã được đăng ký! Vui lòng sử dụng email khác.");
        }

        // Phone
        if (dto.getPhone() != null && !dto.getPhone().isBlank() && dto.getPhone().trim().length() > 20) {
            throw new IllegalArgumentException("Số điện thoại không được vượt quá 20 ký tự!");
        }

        // Password
        if (dto.getPassword() == null || dto.getPassword().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có độ dài tối thiểu 6 ký tự!");
        }
        if (dto.getConfirmPassword() == null || !dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Xác nhận mật khẩu không khớp với mật khẩu đã nhập!");
        }
    }
}
