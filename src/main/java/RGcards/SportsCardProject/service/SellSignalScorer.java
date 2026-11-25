package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dto.AskStats;
import RGcards.SportsCardProject.enums.SellSignalItemStatus;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Pure scoring: how far the low asking price (in TWD) sits above what the user paid. */
public final class SellSignalScorer {

    static final int MIN_LISTINGS = 3;
    /** Listings at which the score stops being discounted for a thin market. */
    static final int FULL_CONFIDENCE_LISTINGS = 5;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private SellSignalScorer() {
    }

    public record Result(SellSignalItemStatus status, BigDecimal gainPct, BigDecimal score) {
    }

    public static Result score(BigDecimal costTwd, AskStats stats, BigDecimal usdToTwd) {
        if (costTwd == null || costTwd.signum() <= 0) {
            return new Result(SellSignalItemStatus.NO_COST, null, null);
        }
        if (stats.count() < MIN_LISTINGS || stats.lowAsk() == null) {
            return new Result(SellSignalItemStatus.TOO_FEW_LISTINGS, null, null);
        }
        BigDecimal lowAskTwd = stats.lowAsk().multiply(usdToTwd);
        BigDecimal gainPct = lowAskTwd.divide(costTwd, MathContext.DECIMAL64)
                .subtract(BigDecimal.ONE)
                .multiply(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal confidence = BigDecimal.valueOf(Math.min(1.0, stats.count() / (double) FULL_CONFIDENCE_LISTINGS));
        BigDecimal score = gainPct.multiply(confidence).setScale(2, RoundingMode.HALF_UP);
        return new Result(SellSignalItemStatus.RANKED, gainPct, score);
    }
}
