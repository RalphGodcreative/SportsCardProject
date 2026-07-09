package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.entity.Recommendation;
import RGcards.SportsCardProject.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recommendation")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public List<Recommendation> findRecommendations() {
        return recommendationService.findCurrent();
    }
}
