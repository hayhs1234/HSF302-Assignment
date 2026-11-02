package com.example.studyhub.controller;

import com.example.studyhub.dtos.StudentRegisterDto;
import com.example.studyhub.repository.DepartmentRepository;
import com.example.studyhub.service.StudentRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class StudentRegisterController {

    private final StudentRegistrationService registrationService;
    private final DepartmentRepository departmentRepository;

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            return isAdmin ? "redirect:/admin/dashboard" : "redirect:/dashboard";
        }

        if (!model.containsAttribute("registerDto")) {
            model.addAttribute("registerDto", new StudentRegisterDto());
        }
        model.addAttribute("departments", departmentRepository.findAllByOrderByNameAsc());
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(
            @ModelAttribute("registerDto") StudentRegisterDto registerDto,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            registrationService.registerStudent(registerDto);
            redirectAttributes.addFlashAttribute("registeredSuccess", true);
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("registerDto", registerDto);
            model.addAttribute("departments", departmentRepository.findAllByOrderByNameAsc());
            return "register";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Đã có lỗi xảy ra trong quá trình đăng ký. Vui lòng thử lại!");
            model.addAttribute("registerDto", registerDto);
            model.addAttribute("departments", departmentRepository.findAllByOrderByNameAsc());
            return "register";
        }
    }
}
