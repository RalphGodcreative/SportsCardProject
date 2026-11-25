package RGcards.SportsCardProject.enums;

public enum SellSignalItemStatus {
    /** Has a cost basis and enough listings; carries a rank. */
    RANKED,
    /** No buy transaction to compute a cost from. */
    NO_COST,
    /** Fewer matched eBay listings than the minimum. */
    TOO_FEW_LISTINGS,
    /** The eBay call for this card failed. */
    EBAY_ERROR
}
