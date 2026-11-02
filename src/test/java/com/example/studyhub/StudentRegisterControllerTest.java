package com.example.studyhub;

import com.example.studyhub.controller.StudentRegisterController;
import com.example.studyhub.dtos.StudentRegisterDto;
import com.example.studyhub.entity.Department;
import com.example.studyhub.entity.Role;
import com.example.studyhub.entity.User;
import com.example.studyhub.repository.DepartmentRepository;
import com.example.studyhub.repository.UserRepository;
import com.example.studyhub.security.CustomAuthenticationSuccessHandler;
import com.example.studyhub.security.CustomUserDetailsService;
import com.example.studyhub.security.SecurityConfig;
import com.example.studyhub.service.StudentRegistrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StudentRegisterController.class)
@Import({SecurityConfig.class, CustomAuthenticationSuccessHandler.class})
class StudentRegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentRegistrationService registrationService;

    @MockitoBean
    private DepartmentRepository departmentRepository;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("Truy cập GET /register khi chưa đăng nhập trả về HTTP 200 và hiển thị form đăng ký")
    void testShowRegisterFormUnauthenticated() throws Exception {
        Department itDept = Department.builder().id(1).name("Công nghệ thông tin").build();
        when(departmentRepository.findAllByOrderByNameAsc()).thenReturn(List.of(itDept));

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("registerDto"))
                .andExpect(model().attributeExists("departments"))
                .andExpect(content().string(containsString("Tạo tài khoản")));
    }

    @Test
    @WithMockUser(username = "sv001", roles = {"STUDENT"})
    @DisplayName("Sinh viên đã đăng nhập truy cập /register thì tự động chuyển hướng về /dashboard")
    void testRegisterRedirectsWhenAlreadyAuthenticated() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("Gửi form POST /register với thông tin hợp lệ thì chuyển hướng về /login?registered=true")
    void testRegisterSuccess() throws Exception {
        User createdUser = User.builder()
                .id(10L)
                .username("sv003")
                .fullName("Nguyễn Văn C")
                .email("sv003@school.edu.vn")
                .role(Role.STUDENT)
                .isActive(true)
                .build();

        when(registrationService.registerStudent(any(StudentRegisterDto.class)))
                .thenReturn(createdUser);

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Nguyễn Văn C")
                        .param("departmentId", "1")
                        .param("username", "sv003")
                        .param("email", "sv003@school.edu.vn")
                        .param("phone", "0912345678")
                        .param("password", "123456")
                        .param("confirmPassword", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"))
                .andExpect(flash().attributeExists("registeredSuccess"));

        verify(registrationService, times(1)).registerStudent(any(StudentRegisterDto.class));
    }

    @Test
    @DisplayName("Gửi form POST /register bị trùng username hiển thị thông báo lỗi trên form")
    void testRegisterFailureDuplicateUsername() throws Exception {
        when(registrationService.registerStudent(any(StudentRegisterDto.class)))
                .thenThrow(new IllegalArgumentException("Tên đăng nhập 'sv001' đã được sử dụng!"));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Trần Văn D")
                        .param("departmentId", "1")
                        .param("username", "sv001")
                        .param("email", "sv004@school.edu.vn")
                        .param("password", "123456")
                        .param("confirmPassword", "123456"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("errorMessage", "Tên đăng nhập 'sv001' đã được sử dụng!"))
                .andExpect(content().string(containsString("register-error-alert")))
                .andExpect(content().string(containsString("sv001")));
    }
}
