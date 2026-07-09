package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.entity.Recommendation;
import RGcards.SportsCardProject.service.RecommendationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recommendation")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public String findRecommendations() throws JsonProcessingException {
        List<Recommendation> recommendations = recommendationService.findAll();
        ObjectMapper om = new ObjectMapper();
        return om.writeValueAsString(recommendations);
    }
}
