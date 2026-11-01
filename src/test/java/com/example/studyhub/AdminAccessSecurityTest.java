package com.example.studyhub;

import com.example.studyhub.controller.admin.AdminDashboardController;
import com.example.studyhub.entity.Role;
import com.example.studyhub.entity.User;
import com.example.studyhub.repository.UserRepository;
import com.example.studyhub.security.CustomAuthenticationSuccessHandler;
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

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminDashboardController.class)
@Import({SecurityConfig.class, CustomAuthenticationSuccessHandler.class})
class AdminAccessSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("Người dùng chưa đăng nhập truy cập /admin/dashboard bị chuyển hướng về /login")
    void testUnauthenticatedAccessRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")));
    }

    @Test
    @WithMockUser(username = "sv001", roles = {"STUDENT"})
    @DisplayName("Sinh viên (STUDENT) cố tình truy cập /admin/dashboard bị từ chối quyền (403/access-denied)")
    void testStudentCannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "gv001", roles = {"LECTURER"})
    @DisplayName("Giảng viên (LECTURER) cố tình truy cập /admin/dashboard bị từ chối quyền (403/access-denied)")
    void testLecturerCannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Quản trị viên (ADMIN) truy cập /admin/dashboard được cấp quyền HTTP 200 và hiển thị trang Admin")
    void testAdminCanAccessAdminDashboard() throws Exception {
        when(userRepository.count()).thenReturn(4L);
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);
        when(userRepository.countByRole(Role.LECTURER)).thenReturn(1L);
        when(userRepository.countByRole(Role.STUDENT)).thenReturn(2L);
        when(userRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("totalUsers", "totalLecturers", "totalStudents"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cổng Quản Trị")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Quản trị viên (ADMIN) truy cập /admin được cấp quyền HTTP 200")
    void testAdminCanAccessAdminRoot() throws Exception {
        when(userRepository.count()).thenReturn(4L);
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);
        when(userRepository.countByRole(Role.LECTURER)).thenReturn(1L);
        when(userRepository.countByRole(Role.STUDENT)).thenReturn(2L);
        when(userRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }
}
