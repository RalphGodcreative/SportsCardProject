package RGcards.SportsCardProject.controller.admin;

import RGcards.SportsCardProject.entity.Recommendation;
import RGcards.SportsCardProject.enums.RecommendationAuthor;
import RGcards.SportsCardProject.enums.RecommendationSport;
import RGcards.SportsCardProject.service.RecommendationAiService;
import RGcards.SportsCardProject.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/recommendations")
@Slf4j
@RequiredArgsConstructor
public class AdminRecommendationController {

    private final RecommendationService recommendationService;
    private final RecommendationAiService recommendationAiService;

    @GetMapping
    public String view(Model model) {
        model.addAttribute("adminCurrent", recommendationService.findCurrentByAuthor(RecommendationAuthor.ADMIN));
        model.addAttribute("aiCurrent", recommendationService.findCurrentByAuthor(RecommendationAuthor.AI));
        model.addAttribute("sports", RecommendationSport.values());
        return "recommendationAdmin";
    }

    @PostMapping
    public String saveManual(@ModelAttribute("player") String player,
                              @ModelAttribute("sport") RecommendationSport sport,
                              @ModelAttribute("reason") String reason) {
        Recommendation recommendation = new Recommendation();
        recommendation.setPlayer(player);
        recommendation.setSport(sport);
        recommendation.setReason(reason);
        recommendationService.saveManual(recommendation);
        return "redirect:/admin/recommendations";
    }

    @PostMapping("/generate")
    public String generate() {
        log.info("Admin triggered AI recommendation generation for all sports");
        recommendationAiService.generateAll();
        log.info("Finished AI recommendation generation for all sports");
        return "redirect:/admin/recommendations";
    }

    @PostMapping("/generate/{sport}")
    public String generateOne(@PathVariable RecommendationSport sport) {
        log.info("Admin triggered AI recommendation generation for sport={}", sport);
        recommendationAiService.generateOne(sport);
        log.info("Finished AI recommendation generation for sport={}", sport);
        return "redirect:/admin/recommendations";
    }
}
