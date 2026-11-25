package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.entity.Card;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * One grounded Gemini call that explains why the top-ranked cards may be worth selling.
 * Prices come from our own data; the model is told not to produce any.
 */
@Service
@RequiredArgsConstructor
public class SellSignalAiService {

    static final String MODEL = "gemini-2.5-flash";
    static final int MAX_REASON_LENGTH = 1000;

    /**
     * "CARD 12: reason", tolerating leading bullets / markdown bold, "#", and full-width colons.
     * JSON mode can't be used: Gemini rejects response_mime_type together with google_search.
     */
    private static final Pattern LINE =
            Pattern.compile("(?m)^[\\s*\\-]*CARD\\s*#?\\s*(\\d+)\\s*\\**\\s*[:：\\-]\\s*(.+?)\\s*$");

    private final GeminiService geminiService;

    public record Candidate(Card card, BigDecimal costTwd, BigDecimal lowAskUsd, BigDecimal gainPct) {
    }

    /** cardId to reason. Cards the model skipped are simply absent. */
    public Map<Integer, String> explain(List<Candidate> candidates) throws JsonProcessingException {
        if (candidates.isEmpty()) {
            return Map.of();
        }
        String raw = geminiService.generateContent(buildPrompt(candidates), MODEL, true);
        Set<Integer> ids = candidates.stream().map(c -> c.card().getId()).collect(Collectors.toSet());
        return parse(raw, ids);
    }

    static String buildPrompt(List<Candidate> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are helping a sports card collector decide which of their cards may be worth selling now.\n")
          .append("The prices below are current eBay ASKING prices (active listings, not sales) and the collector's cost.\n")
          .append("Do not invent, estimate or restate any prices.\n")
          .append("For each card, search for the player's recent news (performance, awards, injuries, team situation, ")
          .append("upcoming events such as playoffs or the draft) and give one or two sentences on whether demand ")
          .append("for this player is likely near a high right now or likely to fade.\n")
          .append("Reply with exactly one line per card and nothing else, in this format:\n")
          .append("CARD <id>: <reason>\n\n");
        for (Candidate c : candidates) {
            Card card = c.card();
            sb.append("CARD ").append(card.getId()).append(": ")
              .append(join(card.getYear(), card.getPublisher(), card.getSet(), card.getPlayer(),
                      card.getInsert(), card.getParallel(), card.getNumbered(),
                      Boolean.TRUE.equals(card.getAuto()) ? "Auto" : null, card.getGrade()))
              .append(" | sport: ").append(orDash(card.getSports()))
              .append(" | cost: ").append(c.costTwd().setScale(0, RoundingMode.HALF_UP)).append(" TWD")
              .append(" | listed from: ").append(c.lowAskUsd().setScale(2, RoundingMode.HALF_UP)).append(" USD")
              .append(" | gain vs cost: ").append(c.gainPct().setScale(0, RoundingMode.HALF_UP)).append("%\n");
        }
        return sb.toString();
    }

    static Map<Integer, String> parse(String raw, Set<Integer> allowedIds) {
        Map<Integer, String> reasons = new HashMap<>();
        if (raw == null) {
            return reasons;
        }
        Matcher m = LINE.matcher(raw);
        while (m.find()) {
            int id;
            try {
                id = Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                continue;
            }
            if (!allowedIds.contains(id) || reasons.containsKey(id)) {
                continue;
            }
            String reason = m.group(2).replace("**", "").trim();
            if (reason.isEmpty()) {
                continue;
            }
            if (reason.length() > MAX_REASON_LENGTH) {
                reason = reason.substring(0, MAX_REASON_LENGTH);
            }
            reasons.put(id, reason);
        }
        return reasons;
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(' ');
                }
                sb.append(p.trim());
            }
        }
        return sb.toString();
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}
