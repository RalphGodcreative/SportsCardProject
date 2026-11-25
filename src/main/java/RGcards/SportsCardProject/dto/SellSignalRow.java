package RGcards.SportsCardProject.dto;

import RGcards.SportsCardProject.entity.Card;
import RGcards.SportsCardProject.entity.SellSignalItem;

import java.math.BigDecimal;
import java.util.List;

/** One card on the Sell Signals page: the saved item, the card it refers to, and TWD conversions. */
public record SellSignalRow(SellSignalItem item, Card card, List<SampleListing> samples,
                            BigDecimal lowAskTwd, BigDecimal medianAskTwd) {
}
