package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.dao.UserRepository;
import RGcards.SportsCardProject.entity.User;
import RGcards.SportsCardProject.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthController {

    @Value("${app.registration.enabled:false}")
    private boolean registrationEnabled;

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        if (!registrationEnabled) return "redirect:/login";
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String email,
            @RequestParam String username,
            @RequestParam String password,
            Model model
    ) {
        if (!registrationEnabled) return "redirect:/login";
        if (!ValidationUtil.isValidEmail(email)) {
            model.addAttribute("error", "Please enter a valid email address.");
            return "register";
        }
        if (!ValidationUtil.isValidPassword(password)) {
            model.addAttribute("error", "Password must be at least 8 characters and contain at least one letter and one number.");
            return "register";
        }
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("ROLE_USER");
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("error", "Email or username already in use.");
            return "register";
        }
        return "redirect:/login?registered";
    }

}
