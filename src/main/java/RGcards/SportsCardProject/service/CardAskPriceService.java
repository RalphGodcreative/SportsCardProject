package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dto.AskStats;
import RGcards.SportsCardProject.dto.EbayItemSummary;
import RGcards.SportsCardProject.dto.SampleListing;
import RGcards.SportsCardProject.entity.Card;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Turns one card into current eBay asking-price stats. These are ACTIVE listings
 * (the Browse API), not completed sales: eBay retired sold-listing search.
 */
@Service
@RequiredArgsConstructor
public class CardAskPriceService {

    static final int SEARCH_LIMIT = 50;
    static final int MAX_QUERY_LENGTH = 100;
    static final int SAMPLE_COUNT = 3;
    static final int SAMPLE_TITLE_LENGTH = 120;

    private static final Pattern GRADED = Pattern.compile("\\b(psa|bgs|sgc|cgc|beckett|graded)\\b");
    private static final Pattern JUNK = Pattern.compile("\\b(lot|lots|reprint|custom|you pick|pick your|choose|u pick)\\b");
    private static final Pattern AUTO = Pattern.compile("\\b(auto|autos|autograph|autographed|signed)\\b");
    private static final Pattern SUFFIX = Pattern.compile("(jr\\.?|sr\\.?|ii|iii|iv)");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private final EbayBrowseService ebayBrowseService;

    /** Throws RestClientException when eBay fails; the caller records that per card. */
    public AskStats lookup(Card card) {
        String query = buildQuery(card);
        if (query.isBlank()) {
            return AskStats.empty(query);
        }
        List<EbayItemSummary> results = ebayBrowseService.searchItems(query, SEARCH_LIMIT);
        return summarize(query, card, results);
    }

    static AskStats summarize(String query, Card card, List<EbayItemSummary> results) {
        List<SampleListing> matched = new ArrayList<>();
        for (EbayItemSummary item : results) {
            if (!"USD".equalsIgnoreCase(item.getCurrency()) || !titleMatches(item.getTitle(), card)) {
                continue;
            }
            BigDecimal price = parsePrice(item.getPrice());
            if (price == null) {
                continue;
            }
            matched.add(new SampleListing(truncate(item.getTitle(), SAMPLE_TITLE_LENGTH), price, item.getItemWebUrl()));
        }
        if (matched.isEmpty()) {
            return AskStats.empty(query);
        }
        matched.sort(Comparator.comparing(SampleListing::price));
        List<BigDecimal> prices = matched.stream().map(SampleListing::price).toList();
        return new AskStats(query, matched.size(), percentile(prices, 0.25), percentile(prices, 0.5),
                List.copyOf(matched.subList(0, Math.min(SAMPLE_COUNT, matched.size()))));
    }

    /** year publisher set player insert parallel print-run auto grade; blanks skipped; max 100 chars. */
    static String buildQuery(Card card) {
        List<String> parts = new ArrayList<>();
        add(parts, card.getYear());
        String set = clean(card.getSet());
        String publisher = clean(card.getPublisher());
        if (publisher != null && (set == null || !lower(set).contains(lower(publisher)))) {
            parts.add(publisher);
        }
        add(parts, set);
        add(parts, card.getPlayer());
        add(parts, card.getInsert());
        add(parts, card.getParallel());
        String printRun = printRun(card.getNumbered());
        if (printRun != null) {
            parts.add(printRun);
        }
        if (Boolean.TRUE.equals(card.getAuto())) {
            parts.add("auto");
        }
        add(parts, card.getGrade());

        StringBuilder query = new StringBuilder();
        for (String part : parts) {
            int extra = (query.isEmpty() ? 0 : 1) + part.length();
            if (query.length() + extra > MAX_QUERY_LENGTH) {
                break;
            }
            if (!query.isEmpty()) {
                query.append(' ');
            }
            query.append(part);
        }
        return query.toString();
    }

    /**
     * Keep a listing only if it looks like the same card: the player's last name is present,
     * same grade (or raw when the card is ungraded), same auto status, same print run,
     * and it is not a lot / reprint / pick-your-card listing.
     */
    static boolean titleMatches(String title, Card card) {
        if (title == null) {
            return false;
        }
        String t = normalize(title);
        if (JUNK.matcher(t).find()) {
            return false;
        }
        String lastName = lastName(card.getPlayer());
        if (lastName != null && !t.contains(lastName)) {
            return false;
        }
        String grade = clean(card.getGrade());
        if (grade == null) {
            if (GRADED.matcher(t).find()) {
                return false;
            }
        } else if (!t.contains(normalize(grade))) {
            return false;
        }
        boolean isAuto = Boolean.TRUE.equals(card.getAuto());
        if (isAuto != AUTO.matcher(t).find()) {
            return false;
        }
        String printRun = printRun(card.getNumbered());
        return printRun == null || t.contains(printRun);
    }

    /** Linear interpolation between closest ranks; input must be sorted ascending and non-empty. */
    static BigDecimal percentile(List<BigDecimal> sorted, double p) {
        if (sorted.size() == 1) {
            return sorted.get(0).setScale(2, RoundingMode.HALF_UP);
        }
        double pos = p * (sorted.size() - 1);
        int lower = (int) Math.floor(pos);
        int upper = (int) Math.ceil(pos);
        BigDecimal lo = sorted.get(lower);
        BigDecimal hi = sorted.get(upper);
        BigDecimal fraction = BigDecimal.valueOf(pos - lower);
        return lo.add(hi.subtract(lo).multiply(fraction)).setScale(2, RoundingMode.HALF_UP);
    }

    /** "23/99", "/99", "#/99" become "/99"; anything without a slash is ignored. */
    static String printRun(String numbered) {
        String n = clean(numbered);
        if (n == null || !n.contains("/")) {
            return null;
        }
        String run = n.substring(n.lastIndexOf('/') + 1).replaceAll("[^0-9]", "");
        return run.isEmpty() ? null : "/" + run;
    }

    private static String lastName(String player) {
        String p = clean(player);
        if (p == null) {
            return null;
        }
        String[] tokens = SPACES.split(normalize(p));
        String last = tokens[tokens.length - 1];
        // "Jr." / "II" style suffixes are often left out of listing titles
        if (tokens.length > 1 && SUFFIX.matcher(last).matches()) {
            last = tokens[tokens.length - 2];
        }
        return last;
    }

    private static BigDecimal parsePrice(String value) {
        if (value == null) {
            return null;
        }
        try {
            BigDecimal price = new BigDecimal(value.trim());
            return price.signum() > 0 ? price : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void add(List<String> parts, String value) {
        String c = clean(value);
        if (c != null) {
            parts.add(c);
        }
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = SPACES.matcher(value.trim()).replaceAll(" ");
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String lower(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        return SPACES.matcher(lower(value)).replaceAll(" ").trim();
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
