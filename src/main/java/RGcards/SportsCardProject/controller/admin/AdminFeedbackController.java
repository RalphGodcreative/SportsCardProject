package RGcards.SportsCardProject.controller.admin;

import RGcards.SportsCardProject.enums.FeedbackType;
import RGcards.SportsCardProject.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/feedback")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping
    public String list(@RequestParam(required = false) FeedbackType type, Model model) {
        model.addAttribute("rows", feedbackService.findAllForAdmin(type));
        model.addAttribute("types", FeedbackType.values());
        model.addAttribute("selectedType", type);
        return "admin/feedback";
    }
}
