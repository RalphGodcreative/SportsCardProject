package RGcards.SportsCardProject.controller.admin;

import RGcards.SportsCardProject.dao.SearchKeywordRepository;
import RGcards.SportsCardProject.dto.AdminUserRow;
import RGcards.SportsCardProject.entity.User;
import RGcards.SportsCardProject.service.AdminUserService;
import RGcards.SportsCardProject.service.CardService;
import RGcards.SportsCardProject.service.UsageLimits;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final CardService cardService;
    private final SearchKeywordRepository searchKeywordRepository;
    private final UsageLimits usageLimits;

    @GetMapping
    public String list(Model model) {
        List<AdminUserRow> rows = adminUserService.findAll().stream()
                .map(user -> new AdminUserRow(
                        user,
                        cardService.findCardsCount(user.getId()),
                        searchKeywordRepository.countByUserId(user.getId()),
                        usageLimits.maxAiCalls(user)
                ))
                .toList();
        model.addAttribute("rows", rows);
        model.addAttribute("roles", List.of("ROLE_USER", "ROLE_TEST", "ROLE_ADMIN"));
        return "admin/users";
    }

    @PostMapping("/{id}/role")
    public String changeRole(@PathVariable Long id, @RequestParam String role,
                              @AuthenticationPrincipal User currentUser,
                              RedirectAttributes redirectAttributes) {
        try {
            adminUserService.changeRole(id, role, currentUser);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/enabled")
    public String setEnabled(@PathVariable Long id, @RequestParam boolean enabled,
                              @AuthenticationPrincipal User currentUser,
                              RedirectAttributes redirectAttributes) {
        try {
            adminUserService.setEnabled(id, enabled, currentUser);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/ai-limit")
    public String setAiLimit(@PathVariable Long id,
                              @RequestParam(required = false) Integer maxAiCalls) {
        adminUserService.setAiCallLimitOverride(id, maxAiCalls);
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal User currentUser,
                          RedirectAttributes redirectAttributes) {
        try {
            adminUserService.deleteUserAndAllData(id, currentUser);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
