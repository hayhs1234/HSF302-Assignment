package com.example.studyhub;

import com.example.studyhub.controller.AuthController;
import com.example.studyhub.controller.DashboardController;
import com.example.studyhub.entity.Role;
import com.example.studyhub.entity.User;
import com.example.studyhub.repository.UserRepository;
import com.example.studyhub.security.CustomUserDetails;
import com.example.studyhub.security.CustomUserDetailsService;
import com.example.studyhub.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuthController.class, DashboardController.class})
@Import({SecurityConfig.class, com.example.studyhub.security.CustomAuthenticationSuccessHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("Truy cập trang /login khi chưa đăng nhập trả về HTTP 200 và hiển thị giao diện đăng nhập")
    void testLoginPageRendersSuccessfully() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("StudyHub")));
    }

    @Test
    @DisplayName("Truy cập trang /login với tham số error hiển thị thông báo lỗi")
    void testLoginPageShowsError() throws Exception {
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("Truy cập trang /login với tham số logout hiển thị thông báo đã đăng xuất")
    void testLoginPageShowsLogout() throws Exception {
        mockMvc.perform(get("/login").param("logout", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("logoutMessage"));
    }

    @Test
    @DisplayName("Truy cập /dashboard khi chưa đăng nhập thì bị chuyển hướng về /login")
    void testDashboardRedirectsWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Truy cập /dashboard khi đã đăng nhập thành công trả về HTTP 200")
    void testDashboardAccessWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    @DisplayName("Admin đăng nhập thành công thì chuyển hướng đến /admin/dashboard")
    void testAdminLoginSuccessRedirectsToAdminDashboard() throws Exception {
        User mockAdmin = User.builder()
                .id(1L)
                .username("admin")
                .passwordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("123456"))
                .fullName("Quản trị viên")
                .email("admin@school.edu.vn")
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        when(userDetailsService.loadUserByUsername("admin"))
                .thenReturn(new CustomUserDetails(mockAdmin));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @DisplayName("Sinh viên đăng nhập thành công thì chuyển hướng đến /dashboard")
    void testStudentLoginSuccessRedirectsToDashboard() throws Exception {
        User mockStudent = User.builder()
                .id(3L)
                .username("sv001")
                .passwordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("123456"))
                .fullName("Trần Thị Bình")
                .email("sv001@school.edu.vn")
                .role(Role.STUDENT)
                .isActive(true)
                .build();

        when(userDetailsService.loadUserByUsername("sv001"))
                .thenReturn(new CustomUserDetails(mockStudent));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "sv001")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi sai mật khẩu chuyển hướng về /login?error=true")
    void testLoginFailureWrongPassword() throws Exception {
        User mockUser = User.builder()
                .id(1L)
                .username("admin")
                .passwordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("123456"))
                .fullName("Quản trị viên")
                .email("admin@school.edu.vn")
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        when(userDetailsService.loadUserByUsername("admin"))
                .thenReturn(new CustomUserDetails(mockUser));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "wrongpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Đăng xuất thành công chuyển hướng về /login?logout=true")
    void testLogoutSuccess() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));
    }
}
