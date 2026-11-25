package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dto.AskStats;
import RGcards.SportsCardProject.enums.SellSignalItemStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SellSignalScorerTest {

    private static final BigDecimal FX = new BigDecimal("32");

    private static AskStats stats(int count, String lowAsk) {
        return new AskStats("q", count, lowAsk == null ? null : new BigDecimal(lowAsk), null, List.of());
    }

    @Test
    void noCostIsNotRanked() {
        SellSignalScorer.Result r = SellSignalScorer.score(null, stats(10, "100"), FX);
        assertEquals(SellSignalItemStatus.NO_COST, r.status());
        assertNull(r.score());
    }

    @Test
    void zeroCostCountsAsNoCost() {
        assertEquals(SellSignalItemStatus.NO_COST,
                SellSignalScorer.score(BigDecimal.ZERO, stats(10, "100"), FX).status());
    }

    @Test
    void fewerThanThreeListingsIsNotRanked() {
        SellSignalScorer.Result r = SellSignalScorer.score(new BigDecimal("1000"), stats(2, "100"), FX);
        assertEquals(SellSignalItemStatus.TOO_FEW_LISTINGS, r.status());
    }

    @Test
    void gainIsLowAskInTwdOverCost() {
        // 100 USD * 32 = 3200 TWD vs 1600 cost = +100%
        SellSignalScorer.Result r = SellSignalScorer.score(new BigDecimal("1600"), stats(5, "100"), FX);
        assertEquals(SellSignalItemStatus.RANKED, r.status());
        assertEquals(new BigDecimal("100.00"), r.gainPct());
        assertEquals(new BigDecimal("100.00"), r.score());
    }

    @Test
    void thinMarketIsDiscounted() {
        // same +100% gain, but only 3 of 5 listings: score = 100 * 0.6
        SellSignalScorer.Result r = SellSignalScorer.score(new BigDecimal("1600"), stats(3, "100"), FX);
        assertEquals(new BigDecimal("100.00"), r.gainPct());
        assertEquals(new BigDecimal("60.00"), r.score());
    }

    @Test
    void lossGivesNegativeGain() {
        // 25 USD * 32 = 800 TWD vs 1600 cost = -50%
        SellSignalScorer.Result r = SellSignalScorer.score(new BigDecimal("1600"), stats(8, "25"), FX);
        assertEquals(new BigDecimal("-50.00"), r.gainPct());
    }
}
