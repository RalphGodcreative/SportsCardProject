package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.dao.SearchKeywordRepository;
import RGcards.SportsCardProject.dao.UserRepository;
import RGcards.SportsCardProject.entity.User;
import RGcards.SportsCardProject.security.AccountLinkSession;
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
    public String editPage(@AuthenticationPrincipal(errorOnInvalidType = false) User user, Model model) {
        if (user == null) return "redirect:/";
        // Read the account back from the DB: the SecurityContext principal is a snapshot
        // from sign-in time, so it still shows the old values right after a save or a
        // Google connect/disconnect.
        User fresh = userRepository.findById(user.getId()).orElse(null);
        if (fresh == null) return "redirect:/";
        model.addAttribute("user", fresh);
        addUsageAttributes(model, fresh);
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

    /**
     * Starts the Google round trip in "linking" mode. A POST so Spring Security's CSRF
     * token protects it &mdash; nobody can be walked into connecting an account by a link.
     */
    @PostMapping("/link/google")
    public String linkGoogle(
            @AuthenticationPrincipal(errorOnInvalidType = false) User currentUser,
            HttpServletRequest request
    ) {
        if (currentUser == null) return "redirect:/";
        AccountLinkSession.start(request, currentUser.getId());
        return "redirect:/oauth2/authorization/google";
    }

    @PostMapping("/unlink/google")
    public String unlinkGoogle(
            @AuthenticationPrincipal(errorOnInvalidType = false) User currentUser,
            RedirectAttributes redirectAttributes
    ) {
        if (currentUser == null) return "redirect:/";

        User user = userRepository.findById(currentUser.getId()).orElseThrow();
        if (user.getGoogleSub() == null) {
            return "redirect:/user/edit";
        }
        // Disconnecting the only way in would lock the account out for good.
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            redirectAttributes.addFlashAttribute("error",
                    "Set a password first, otherwise disconnecting Google would leave you no way to sign in.");
            return "redirect:/user/edit";
        }

        user.setGoogleSub(null);
        userRepository.save(user);
        return "redirect:/user/edit?unlinked";
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
