package com.example.studyhub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StudyhubApplicationTests {

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.studyhub.service.StudentRegistrationService registrationService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.studyhub.repository.UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.studyhub.repository.StudentRepository studentRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void testRealDatabaseRegistration() {
        String uniqueSuffix = String.valueOf(System.currentTimeMillis()).substring(7);
        com.example.studyhub.dtos.StudentRegisterDto dto = com.example.studyhub.dtos.StudentRegisterDto.builder()
                .fullName("Test Student " + uniqueSuffix)
                .departmentId(1)
                .username("svtest" + uniqueSuffix)
                .email("svtest" + uniqueSuffix + "@school.edu.vn")
                .password("password123")
                .confirmPassword("password123")
                .build();

        com.example.studyhub.entity.User createdUser = registrationService.registerStudent(dto);
        org.junit.jupiter.api.Assertions.assertNotNull(createdUser.getId());

        java.util.Optional<com.example.studyhub.entity.Student> studentOpt = studentRepository.findById(createdUser.getId());
        org.junit.jupiter.api.Assertions.assertTrue(studentOpt.isPresent());
        org.junit.jupiter.api.Assertions.assertNotNull(studentOpt.get().getStudentCode());
        org.junit.jupiter.api.Assertions.assertTrue(studentOpt.get().getStudentCode().startsWith("SV"));

        // Cleanup after test
        studentRepository.delete(studentOpt.get());
        userRepository.delete(createdUser);
    }

}
