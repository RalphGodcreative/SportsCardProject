package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.dao.SearchKeywordRepository;
import RGcards.SportsCardProject.dao.UserRepository;
import RGcards.SportsCardProject.entity.User;
import RGcards.SportsCardProject.service.AccountDeletionService;
import RGcards.SportsCardProject.service.CardService;
import RGcards.SportsCardProject.service.UsageLimits;
import RGcards.SportsCardProject.util.ValidationUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final CardService cardService;
    private final SearchKeywordRepository searchKeywordRepository;
    private final UsageLimits usageLimits;
    private final AccountDeletionService accountDeletionService;

    @GetMapping("/edit")
    public String editPage(@AuthenticationPrincipal User user, Model model) {
        if (user == null) return "redirect:/";
        model.addAttribute("user", user);
        addUsageAttributes(model, user);
        return "edit-user";
    }

    private void addUsageAttributes(Model model, User user) {
        model.addAttribute("cardCount", cardService.findCardsCount(user.getId()));
        model.addAttribute("cardLimit", usageLimits.maxCards(user));
        model.addAttribute("keywordCount", searchKeywordRepository.countByUserId(user.getId()));
        model.addAttribute("keywordLimit", usageLimits.maxKeywords(user));
        model.addAttribute("aiCallCount", user.getAiCallCount());
        model.addAttribute("aiCallLimit", usageLimits.maxAiCalls(user));
    }

    @PostMapping("/edit")
    public String edit(
            @AuthenticationPrincipal(errorOnInvalidType = false) User currentUser,
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam(required = false) String password,
            Model model
    ) {
        if (currentUser == null) return "redirect:/";
        email = email.toLowerCase();
        if (userRepository.existsByUsernameAndIdNot(username, currentUser.getId())) {
            model.addAttribute("user", currentUser);
            model.addAttribute("error", "Username is already taken.");
            addUsageAttributes(model, currentUser);
            return "edit-user";
        }
        if (userRepository.existsByEmailAndIdNot(email, currentUser.getId())) {
            model.addAttribute("user", currentUser);
            model.addAttribute("error", "Email is already in use.");
            addUsageAttributes(model, currentUser);
            return "edit-user";
        }
        if (!ValidationUtil.isValidEmail(email)) {
            model.addAttribute("user", currentUser);
            model.addAttribute("error", "Please enter a valid email address.");
            addUsageAttributes(model, currentUser);
            return "edit-user";
        }
        if (password != null && !password.isBlank() && !ValidationUtil.isValidPassword(password)) {
            model.addAttribute("user", currentUser);
            model.addAttribute("error", "Password must be at least 8 characters and contain at least one letter and one number.");
            addUsageAttributes(model, currentUser);
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

    @PostMapping("/delete")
    public String deleteOwnAccount(
            @AuthenticationPrincipal(errorOnInvalidType = false) User currentUser,
            @RequestParam String confirmUsername,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        if (currentUser == null) return "redirect:/";

        // The form makes the user type their own username; re-check it here so a
        // stray POST can never wipe an account on its own.
        if (!currentUser.getDisplayName().equals(confirmUsername)) {
            redirectAttributes.addFlashAttribute("error",
                    "The username you typed did not match, so nothing was deleted.");
            return "redirect:/user/edit";
        }

        try {
            accountDeletionService.deleteUserAndAllData(currentUser.getId());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/edit";
        }

        new SecurityContextLogoutHandler().logout(request, response,
                SecurityContextHolder.getContext().getAuthentication());
        return "redirect:/login?deleted";
    }

}
