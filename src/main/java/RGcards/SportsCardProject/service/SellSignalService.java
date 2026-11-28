package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dao.CardRepository;
import RGcards.SportsCardProject.dao.SellSignalRunRepository;
import RGcards.SportsCardProject.dto.AskStats;
import RGcards.SportsCardProject.dto.OwnedCardCost;
import RGcards.SportsCardProject.dto.SampleListing;
import RGcards.SportsCardProject.dto.SellSignalPage;
import RGcards.SportsCardProject.dto.SellSignalRow;
import RGcards.SportsCardProject.entity.Card;
import RGcards.SportsCardProject.entity.SellSignalItem;
import RGcards.SportsCardProject.entity.SellSignalRun;
import RGcards.SportsCardProject.entity.User;
import RGcards.SportsCardProject.enums.CardType;
import RGcards.SportsCardProject.enums.SellSignalAiStatus;
import RGcards.SportsCardProject.enums.SellSignalItemStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scans a user's owned cards against current eBay asking prices, ranks them by gain over
 * cost, and asks Gemini (once) to explain the top few. Every run is saved.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellSignalService {

    static final int MAX_CARDS_PER_RUN = 20;
    static final int AI_TOP_N = 5;
    static final Duration COOLDOWN = Duration.ofHours(24);
    static final int MAX_SAMPLE_JSON_LENGTH = 4000;
    static final int MAX_QUERY_LENGTH = 255;

    private final CardRepository cardRepository;
    private final SellSignalRunRepository runRepository;
    private final CardAskPriceService askPriceService;
    private final SellSignalAiService aiService;
    private final AiCallGuard aiCallGuard;
    private final AppSettingService appSettingService;
    private final ObjectMapper objectMapper;

    /** Users with a scan in progress, so a double submit doesn't start a second one. */
    private final Set<Long> running = ConcurrentHashMap.newKeySet();

    /** started is false when the cooldown or an in-progress scan stopped a new one. */
    public record RunOutcome(SellSignalRun run, boolean started) {
    }

    public Optional<SellSignalRun> latest(Long userId) {
        return runRepository.findFirstByUserIdOrderByCreatedAtDesc(userId);
    }

    /** Admins skip the cooldown (the feature is admin-only while match quality is tuned). */
    public static boolean isCooldownExempt(User user) {
        return "ROLE_ADMIN".equals(user.getRole());
    }

    /** When the user may run again, or null if they can run now. */
    public static LocalDateTime nextRunAt(User user, SellSignalRun latest, LocalDateTime now) {
        if (latest == null || isCooldownExempt(user)) {
            return null;
        }
        LocalDateTime next = latest.getCreatedAt().plus(COOLDOWN);
        return next.isAfter(now) ? next : null;
    }

    public RunOutcome run(User principal) {
        Long userId = principal.getId();
        SellSignalRun previous = latest(userId).orElse(null);
        if (nextRunAt(principal, previous, LocalDateTime.now()) != null) {
            return new RunOutcome(previous, false);
        }
        if (!running.add(userId)) {
            return new RunOutcome(previous, false);
        }
        try {
            return new RunOutcome(scan(principal), true);
        } finally {
            running.remove(userId);
        }
    }

    /**
     * No transaction is held across the eBay and Gemini calls; the run and its items
     * are written in a single save at the end.
     */
    SellSignalRun scan(User principal) {
        Long userId = principal.getId();
        Map<Integer, BigDecimal> costs = ownedCardCosts(userId);
        List<Card> cards = cardRepository.findByUserIdOrderByIdDesc(userId).stream()
                .filter(c -> costs.containsKey(c.getId()))
                .sorted(selectionOrder(costs))
                .limit(MAX_CARDS_PER_RUN)
                .toList();
        BigDecimal fx = appSettingService.getUsdToTwd();

        SellSignalRun run = new SellSignalRun();
        run.setUserId(userId);
        run.setCreatedAt(LocalDateTime.now());
        run.setCardsConsidered(cards.size());
        run.setUsdToTwd(fx);

        Map<Integer, Card> cardsById = new HashMap<>();
        for (Card card : cards) {
            cardsById.put(card.getId(), card);
            run.addItem(priceCard(card, costs.get(card.getId()), fx));
        }

        List<SellSignalItem> ranked = run.getItems().stream()
                .filter(i -> i.getStatus() == SellSignalItemStatus.RANKED)
                .sorted(Comparator.comparing(SellSignalItem::getScore).reversed()
                        .thenComparing(SellSignalItem::getGainPct, Comparator.reverseOrder()))
                .toList();
        for (int i = 0; i < ranked.size(); i++) {
            ranked.get(i).setRank((short) (i + 1));
        }

        // Only cards above cost are sell candidates worth an explanation.
        List<SellSignalItem> top = ranked.stream()
                .filter(i -> i.getGainPct().signum() > 0)
                .limit(AI_TOP_N)
                .toList();
        run.setAiStatus(explainTop(principal, top, cardsById));
        SellSignalRun saved = runRepository.save(run);
        log.info("Sell signals run {} for user {}: {} cards, {} ranked, AI {}",
                saved.getId(), userId, cards.size(), ranked.size(), saved.getAiStatus());
        return saved;
    }

    private SellSignalItem priceCard(Card card, BigDecimal cost, BigDecimal fx) {
        SellSignalItem item = new SellSignalItem();
        item.setCardId(card.getId());
        item.setCostBasisTwd(cost == null ? null : cost.setScale(2, RoundingMode.HALF_UP));

        AskStats stats;
        try {
            stats = askPriceService.lookup(card);
        } catch (RuntimeException e) {
            log.warn("eBay lookup failed for card {}: {}", card.getId(), e.getMessage());
            item.setStatus(SellSignalItemStatus.EBAY_ERROR);
            item.setEbayQuery(truncate(CardAskPriceService.buildQuery(card), MAX_QUERY_LENGTH));
            return item;
        }

        item.setEbayQuery(truncate(stats.query(), MAX_QUERY_LENGTH));
        item.setListingCount(stats.count());
        item.setLowAskUsd(stats.lowAsk());
        item.setMedianAskUsd(stats.median());
        item.setSampleListings(samplesJson(stats.samples()));

        SellSignalScorer.Result result = SellSignalScorer.score(cost, stats, fx);
        item.setStatus(result.status());
        item.setGainPct(result.gainPct());
        item.setScore(result.score());
        return item;
    }

    private SellSignalAiStatus explainTop(User principal, List<SellSignalItem> top, Map<Integer, Card> cardsById) {
        if (top.isEmpty() || !aiCallGuard.hasRemaining(principal)) {
            return SellSignalAiStatus.SKIPPED;
        }
        List<SellSignalAiService.Candidate> candidates = top.stream()
                .map(i -> new SellSignalAiService.Candidate(
                        cardsById.get(i.getCardId()), i.getCostBasisTwd(), i.getLowAskUsd(), i.getGainPct()))
                .toList();
        try {
            Map<Integer, String> reasons = aiService.explain(candidates);
            aiCallGuard.recordCall(principal);
            for (SellSignalItem item : top) {
                item.setAiReason(reasons.get(item.getCardId()));
            }
            return SellSignalAiStatus.OK;
        } catch (Exception e) {
            log.warn("Sell signals AI call failed for user {}: {}", principal.getId(), e.getMessage());
            return SellSignalAiStatus.FAILED;
        }
    }

    /** Card id to cost; a card with several IN rows keeps the first non-null cost. */
    private Map<Integer, BigDecimal> ownedCardCosts(Long userId) {
        Map<Integer, BigDecimal> costs = new LinkedHashMap<>();
        for (OwnedCardCost row : cardRepository.findOwnedCardCosts(userId)) {
            Integer id = row.getCardId();
            if (costs.containsKey(id)) {
                log.warn("Card {} has more than one IN transaction; using the first cost", id);
                if (costs.get(id) != null) {
                    continue;
                }
            }
            costs.put(id, row.getCostBasis());
        }
        return costs;
    }

    /** INVESTMENT cards first, then highest cost (unknown cost last), then newest. */
    static Comparator<Card> selectionOrder(Map<Integer, BigDecimal> costs) {
        Comparator<Card> investmentFirst = Comparator.comparing(c -> c.getType() == CardType.INVESTMENT ? 0 : 1);
        Comparator<Card> costDesc = Comparator.comparing(c -> costs.get(c.getId()),
                Comparator.nullsLast(Comparator.reverseOrder()));
        Comparator<Card> newest = Comparator.comparing(Card::getId, Comparator.reverseOrder());
        return investmentFirst.thenComparing(costDesc).thenComparing(newest);
    }

    private String samplesJson(List<SampleListing> samples) {
        List<SampleListing> list = new ArrayList<>(samples);
        while (!list.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(list);
                if (json.length() <= MAX_SAMPLE_JSON_LENGTH) {
                    return json;
                }
            } catch (JsonProcessingException e) {
                log.warn("Could not serialize sample listings: {}", e.getMessage());
                return null;
            }
            list.remove(list.size() - 1);
        }
        return null;
    }

    // ---------- page ----------

    public SellSignalPage page(User principal) {
        SellSignalRun run = latest(principal.getId()).orElse(null);
        if (run == null) {
            return SellSignalPage.empty();
        }
        Map<Integer, Card> cards = new HashMap<>();
        List<Integer> ids = run.getItems().stream().map(SellSignalItem::getCardId).toList();
        for (Card card : cardRepository.findAllById(ids)) {
            if (principal.getId().equals(card.getUserId())) {
                cards.put(card.getId(), card);
            }
        }

        List<SellSignalRow> ranked = new ArrayList<>();
        List<SellSignalRow> noCost = new ArrayList<>();
        List<SellSignalRow> unpriced = new ArrayList<>();
        for (SellSignalItem item : run.getItems()) {
            Card card = cards.get(item.getCardId());
            if (card == null) {
                continue;
            }
            SellSignalRow row = new SellSignalRow(item, card, parseSamples(item.getSampleListings()),
                    toTwd(item.getLowAskUsd(), run.getUsdToTwd()), toTwd(item.getMedianAskUsd(), run.getUsdToTwd()));
            switch (item.getStatus()) {
                case RANKED -> ranked.add(row);
                case NO_COST -> noCost.add(row);
                default -> unpriced.add(row);
            }
        }
        ranked.sort(Comparator.comparing(r -> r.item().getRank()));
        noCost.sort(Comparator.comparing(r -> r.item().getLowAskUsd(), Comparator.nullsLast(Comparator.reverseOrder())));
        return new SellSignalPage(run, ranked, noCost, unpriced, nextRunAt(principal, run, LocalDateTime.now()));
    }

    private List<SampleListing> parseSamples(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<SampleListing>>() { });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private static BigDecimal toTwd(BigDecimal usd, BigDecimal fx) {
        return usd == null || fx == null ? null : usd.multiply(fx).setScale(0, RoundingMode.HALF_UP);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
