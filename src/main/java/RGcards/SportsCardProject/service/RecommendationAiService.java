package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dao.RecommendationRepository;
import RGcards.SportsCardProject.entity.Recommendation;
import RGcards.SportsCardProject.enums.RecommendationAuthor;
import RGcards.SportsCardProject.enums.RecommendationSport;
import RGcards.SportsCardProject.util.DataProcessUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendationAiService {

    private final GeminiService geminiService;
    private final RecommendationRepository recommendationRepository;

    public void generateAll() {
        for (RecommendationSport sport : RecommendationSport.values()) {
            generateOne(sport);
        }
    }

    public void generateOne(RecommendationSport sport) {
        try {
            generate(sport);
        } catch (Exception e) {
            log.error("AI recommendation for sport={} failed, keeping existing recommendation: {}", sport, e.getMessage(), e);
        }
    }

    private void generate(RecommendationSport sport) throws Exception {
        log.info("Requesting AI recommendation for sport={}", sport);

        String prompt = String.format(
            "You are a sports card investment analyst specializing in %s. Pick ONE current or " +
            "rising player in this sport who is worth watching for trading card investment right now. " +
            "Research current stats, recent performance, and market trends before answering.\n\n" +
            "Reply using EXACTLY this format, no markdown, no extra text:\n" +
            "Player: <full player name>\n" +
            "Reason: <70-90 character reason>",
            sport
        );

        String raw = geminiService.generateContent(prompt, "gemini-2.5-flash", true);
        log.info("Gemini raw response for sport={}: {}", sport, raw);

        String player = extractField(raw, "Player:");
        String reason = truncate(extractField(raw, "Reason:"), 120);

        if (player == null || reason == null) {
            log.warn("AI response for sport={} did not match the expected format, keeping existing recommendation. raw={}", sport, raw);
            return;
        }

        Recommendation recommendation = new Recommendation();
        recommendation.setSport(sport);
        recommendation.setPlayer(DataProcessUtil.upperCaseFirstLetter(player));
        recommendation.setReason(reason);
        recommendation.setAuthor(RecommendationAuthor.AI);
        recommendation.setCreatedAt(LocalDateTime.now());

        Recommendation saved = recommendationRepository.save(recommendation);
        log.info("Saved AI recommendation id={} sport={} player={} reason={}",
            saved.getId(), sport, saved.getPlayer(), reason);
    }

    private String extractField(String text, String label) {
        if (text == null) return null;
        Matcher matcher = Pattern.compile("(?im)^" + Pattern.quote(label) + "\\s*(.+)$").matcher(text);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength);
    }
}
