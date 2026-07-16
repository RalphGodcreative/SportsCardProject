package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dao.RecommendationRepository;
import RGcards.SportsCardProject.entity.Recommendation;
import RGcards.SportsCardProject.enums.RecommendationAuthor;
import RGcards.SportsCardProject.enums.RecommendationSport;
import RGcards.SportsCardProject.util.DataProcessUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int REASON_MAX_LENGTH = 120;

    private final RecommendationRepository recommendationRepository;

    public List<Recommendation> findCurrent() {
        List<Recommendation> current = new ArrayList<>();
        for (RecommendationAuthor author : RecommendationAuthor.values()) {
            for (Recommendation recommendation : findCurrentByAuthor(author).values()) {
                if (recommendation != null) {
                    current.add(recommendation);
                }
            }
        }
        return current;
    }

    public Recommendation saveManual(Recommendation recommendation) {
        recommendation.setPlayer(DataProcessUtil.upperCaseFirstLetter(recommendation.getPlayer()));
        recommendation.setReason(truncate(recommendation.getReason(), REASON_MAX_LENGTH));
        recommendation.setAuthor(RecommendationAuthor.ADMIN);
        recommendation.setCreatedAt(LocalDateTime.now());
        return recommendationRepository.save(recommendation);
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength);
    }

    public Map<RecommendationSport, Recommendation> findCurrentByAuthor(RecommendationAuthor author) {
        Map<RecommendationSport, Recommendation> current = new LinkedHashMap<>();
        for (RecommendationSport sport : RecommendationSport.values()) {
            current.put(sport, recommendationRepository
                    .findFirstBySportAndAuthorOrderByCreatedAtDesc(sport, author)
                    .orElse(null));
        }
        return current;
    }
}
