package com.example.studyhub.controller.admin;

import com.example.studyhub.entity.Role;
import com.example.studyhub.entity.User;
import com.example.studyhub.repository.UserRepository;
import com.example.studyhub.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final UserRepository userRepository;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            model.addAttribute("adminName", userDetails.getFullName());
            model.addAttribute("adminUsername", userDetails.getUsername());
            model.addAttribute("adminEmail", userDetails.getEmail());
        }

        // Summary statistics
        long totalUsers = userRepository.count();
        long totalLecturers = userRepository.countByRole(Role.LECTURER);
        long totalStudents = userRepository.countByRole(Role.STUDENT);
        long totalAdmins = userRepository.countByRole(Role.ADMIN);

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalLecturers", totalLecturers);
        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("totalAdmins", totalAdmins);

        // Recent users
        List<User> recentUsers = userRepository.findTop10ByOrderByCreatedAtDesc();
        model.addAttribute("recentUsers", recentUsers);

        return "admin/dashboard";
    }
}
