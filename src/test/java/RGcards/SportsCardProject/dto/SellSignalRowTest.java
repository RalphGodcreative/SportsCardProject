package RGcards.SportsCardProject.dto;

import RGcards.SportsCardProject.entity.SellSignalItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellSignalRowTest {

    private static SellSignalRow row(String cost, String lowTwd, String medianTwd) {
        SellSignalItem item = new SellSignalItem();
        item.setCostBasisTwd(cost == null ? null : new BigDecimal(cost));
        return new SellSignalRow(item, null, List.of(),
                lowTwd == null ? null : new BigDecimal(lowTwd),
                medianTwd == null ? null : new BigDecimal(medianTwd));
    }

    @Test
    void aboveCostFillsFromCostToLowestAsk() {
        // max = 1120 * 1.12 = 1254.4
        SellSignalRow.Ladder l = row("500", "1000", "1120").ladder();
        assertTrue(l.up());
        assertEquals(39.86, l.costPct());
        assertEquals(39.86, l.fillLeftPct());
        assertEquals(39.86, l.fillWidthPct());
        assertEquals(89.29, l.medianPct());
    }

    @Test
    void belowCostFillsFromLowestAskToCost() {
        // max = 1000 * 1.12 = 1120
        SellSignalRow.Ladder l = row("1000", "600", null).ladder();
        assertFalse(l.up());
        assertEquals(89.29, l.costPct());
        assertEquals(53.57, l.fillLeftPct());
        assertEquals(35.72, l.fillWidthPct());
        assertNull(l.medianPct());
    }

    @Test
    void noLadderWithoutCostOrAsk() {
        assertNull(row(null, "600", "700").ladder());
        assertNull(row("1000", null, null).ladder());
    }
}
