package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.entity.User;
import RGcards.SportsCardProject.enums.FeedbackType;
import RGcards.SportsCardProject.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping
    public String page(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("types", FeedbackType.values());
        model.addAttribute("myFeedback", feedbackService.findForUser(user.getId()));
        return "feedback/form";
    }

    @PostMapping
    public String submit(@AuthenticationPrincipal User user,
                         @RequestParam FeedbackType type,
                         @RequestParam String content,
                         RedirectAttributes redirectAttributes) {
        try {
            feedbackService.submit(user.getId(), type, content);
            redirectAttributes.addFlashAttribute("success", "感謝你的回饋！");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("draft", content);
        }
        return "redirect:/feedback";
    }
}
