package RGcards.SportsCardProject.dto;

import RGcards.SportsCardProject.entity.SellSignalRun;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Everything the Sell Signals page shows for one run, split into ranked cards,
 * cards with no cost basis, and cards that could not be priced.
 * nextRunAt is null when the user can run a scan now.
 */
public record SellSignalPage(SellSignalRun run, List<SellSignalRow> ranked, List<SellSignalRow> noCost,
                             List<SellSignalRow> unpriced, LocalDateTime nextRunAt) {

    public static SellSignalPage empty() {
        return new SellSignalPage(null, List.of(), List.of(), List.of(), null);
    }
}
