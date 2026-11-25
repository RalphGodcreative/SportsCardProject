package RGcards.SportsCardProject.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Current eBay asking prices (USD) for one card, after title filtering.
 * lowAsk is the 25th percentile; both prices are null when nothing matched.
 */
public record AskStats(String query, int count, BigDecimal lowAsk, BigDecimal median, List<SampleListing> samples) {

    public static AskStats empty(String query) {
        return new AskStats(query, 0, null, null, List.of());
    }
}
