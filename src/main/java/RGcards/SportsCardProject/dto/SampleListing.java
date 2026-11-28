package RGcards.SportsCardProject.dto;

import java.math.BigDecimal;

/** One eBay listing shown so the user can check the comps a signal was based on. */
public record SampleListing(String title, BigDecimal price, String url) {
}
