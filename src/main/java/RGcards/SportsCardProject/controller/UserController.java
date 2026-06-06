package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.dao.UserRepository;
import RGcards.SportsCardProject.entity.User;
import RGcards.SportsCardProject.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/edit")
    public String editPage(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("user", user);
        return "edit-user";
    }

    @PostMapping("/edit")
    public String edit(
            @AuthenticationPrincipal User currentUser,
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam(required = false) String password,
            Model model
    ) {
        if (userRepository.existsByUsernameAndIdNot(username, currentUser.getId())) {
            model.addAttribute("user", currentUser);
            model.addAttribute("error", "Username is already taken.");
            return "edit-user";
        }
        if (userRepository.existsByEmailAndIdNot(email, currentUser.getId())) {
            model.addAttribute("user", currentUser);
            model.addAttribute("error", "Email is already in use.");
            return "edit-user";
        }
        if (!ValidationUtil.isValidEmail(email)) {
            model.addAttribute("user", currentUser);
            model.addAttribute("error", "Please enter a valid email address.");
            return "edit-user";
        }
        if (password != null && !password.isBlank() && !ValidationUtil.isValidPassword(password)) {
            model.addAttribute("user", currentUser);
            model.addAttribute("error", "Password must be at least 8 characters and contain at least one letter and one number.");
            return "edit-user";
        }

        // Load a fresh copy from DB so the live SecurityContext principal is never mutated
        User user = userRepository.findById(currentUser.getId()).orElseThrow();
        user.setUsername(username);
        user.setEmail(email);
        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        userRepository.save(user);

        return "redirect:/user/edit?saved";
    }

}
