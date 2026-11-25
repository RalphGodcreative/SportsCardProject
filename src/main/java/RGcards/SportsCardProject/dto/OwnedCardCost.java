package RGcards.SportsCardProject.dto;

import java.math.BigDecimal;

/** Projection: an owned (not sold) card and its cost basis in TWD; null cost = no buy transaction. */
public interface OwnedCardCost {
    Integer getCardId();
    BigDecimal getCostBasis();
}
