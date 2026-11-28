package RGcards.SportsCardProject.dto;

import RGcards.SportsCardProject.entity.Card;
import RGcards.SportsCardProject.entity.SellSignalItem;

import java.math.BigDecimal;
import java.util.List;

/** One card on the Cards to Sell Now page: the saved item, the card it refers to, and TWD conversions. */
public record SellSignalRow(SellSignalItem item, Card card, List<SampleListing> samples,
                            BigDecimal lowAskTwd, BigDecimal medianAskTwd) {

    /** Headroom past the largest value so the median dot never sits on the bar's edge. */
    static final double LADDER_HEADROOM = 1.12;

    /**
     * Positions, as % of the bar width, for the cost → asking-price bar. The fill runs from cost
     * to the lowest ask; medianPct is null when there is no median.
     */
    public record Ladder(double costPct, double fillLeftPct, double fillWidthPct, Double medianPct, boolean up) {
    }

    /** Null when cost or the lowest ask is missing (no-cost and unpriced rows). */
    public Ladder ladder() {
        BigDecimal cost = item.getCostBasisTwd();
        if (cost == null || lowAskTwd == null) {
            return null;
        }
        double c = cost.doubleValue();
        double low = lowAskTwd.doubleValue();
        Double median = medianAskTwd == null ? null : medianAskTwd.doubleValue();
        double max = Math.max(Math.max(c, low), median == null ? 0 : median) * LADDER_HEADROOM;
        if (max <= 0) {
            return null;
        }
        double costPct = pct(c, max);
        double lowPct = pct(low, max);
        return new Ladder(costPct, Math.min(costPct, lowPct), round2(Math.abs(lowPct - costPct)),
                median == null ? null : pct(median, max), low > c);
    }

    private static double pct(double value, double max) {
        return round2(value / max * 100);
    }

    private static double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
