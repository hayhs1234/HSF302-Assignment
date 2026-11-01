package com.example.studyhub.controller;

import com.example.studyhub.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            model.addAttribute("username", userDetails.getUsername());
            model.addAttribute("fullName", userDetails.getFullName());
            model.addAttribute("email", userDetails.getEmail());
            model.addAttribute("role", userDetails.getRole().name());
        }
        return "dashboard";
    }
}
