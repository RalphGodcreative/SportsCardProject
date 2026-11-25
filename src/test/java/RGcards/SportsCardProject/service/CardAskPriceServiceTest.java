package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dto.AskStats;
import RGcards.SportsCardProject.dto.EbayItemSummary;
import RGcards.SportsCardProject.entity.Card;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardAskPriceServiceTest {

    private static Card card(String player, String grade, Boolean auto, String numbered) {
        Card c = new Card();
        c.setYear("2018");
        c.setPublisher("Topps");
        c.setSet("Topps Chrome");
        c.setPlayer(player);
        c.setGrade(grade);
        c.setAuto(auto);
        c.setNumbered(numbered);
        return c;
    }

    private static EbayItemSummary listing(String title, String price, String currency) {
        EbayItemSummary s = new EbayItemSummary();
        s.setTitle(title);
        s.setPrice(price);
        s.setCurrency(currency);
        s.setItemWebUrl("https://www.ebay.com/itm/1");
        return s;
    }

    @Test
    void queryJoinsFieldsSkipsBlanksAndDuplicatePublisher() {
        Card c = card("Shohei Ohtani", "PSA 10", true, "23/99");
        c.setParallel("  Refractor ");
        c.setInsert("");
        assertEquals("2018 Topps Chrome Shohei Ohtani Refractor /99 auto PSA 10", CardAskPriceService.buildQuery(c));
    }

    @Test
    void queryAddsPublisherWhenSetDoesNotContainIt() {
        Card c = card("Shohei Ohtani", null, null, null);
        c.setSet("Chrome");
        assertEquals("2018 Topps Chrome Shohei Ohtani", CardAskPriceService.buildQuery(c));
    }

    @Test
    void queryIsCappedAtOneHundredCharsOnAWordBoundary() {
        Card c = card("Shohei Ohtani", null, null, null);
        c.setParallel("x".repeat(90));
        String q = CardAskPriceService.buildQuery(c);
        assertTrue(q.length() <= CardAskPriceService.MAX_QUERY_LENGTH);
        assertEquals("2018 Topps Chrome Shohei Ohtani", q);
    }

    @Test
    void printRunIsNormalised() {
        assertEquals("/99", CardAskPriceService.printRun("23/99"));
        assertEquals("/99", CardAskPriceService.printRun("#/99"));
        assertNull(CardAskPriceService.printRun("1 of 1"));
        assertNull(CardAskPriceService.printRun(null));
    }

    @Test
    void rawCardRejectsGradedListings() {
        Card raw = card("Shohei Ohtani", null, false, null);
        assertTrue(CardAskPriceService.titleMatches("2018 Topps Chrome Shohei Ohtani RC #150", raw));
        assertFalse(CardAskPriceService.titleMatches("2018 Topps Chrome Shohei Ohtani RC PSA 10", raw));
    }

    @Test
    void gradedCardNeedsSameGrade() {
        Card graded = card("Shohei Ohtani", "PSA 10", false, null);
        assertTrue(CardAskPriceService.titleMatches("2018 Topps Chrome OHTANI rc psa  10 gem mint", graded));
        assertFalse(CardAskPriceService.titleMatches("2018 Topps Chrome Ohtani PSA 9", graded));
    }

    @Test
    void autoStatusMustMatch() {
        Card base = card("Shohei Ohtani", null, false, null);
        Card auto = card("Shohei Ohtani", null, true, null);
        assertFalse(CardAskPriceService.titleMatches("2018 Topps Chrome Ohtani Auto RC", base));
        assertTrue(CardAskPriceService.titleMatches("2018 Topps Chrome Ohtani Autograph RC", auto));
        assertFalse(CardAskPriceService.titleMatches("2018 Topps Chrome Ohtani RC", auto));
    }

    @Test
    void lotsReprintsWrongPlayerAndPrintRunAreRejected() {
        Card c = card("Ken Griffey Jr.", null, false, "/99");
        assertTrue(CardAskPriceService.titleMatches("Griffey Refractor /99", c));
        assertFalse(CardAskPriceService.titleMatches("Griffey Refractor /199 lot of 3", c));
        assertFalse(CardAskPriceService.titleMatches("Griffey reprint /99", c));
        assertFalse(CardAskPriceService.titleMatches("Griffey Refractor /50", c));
        assertFalse(CardAskPriceService.titleMatches("Bonds Refractor /99", c));
    }

    @Test
    void percentileInterpolates() {
        List<BigDecimal> prices = List.of(new BigDecimal("10"), new BigDecimal("20"),
                new BigDecimal("30"), new BigDecimal("40"), new BigDecimal("50"));
        assertEquals(new BigDecimal("20.00"), CardAskPriceService.percentile(prices, 0.25));
        assertEquals(new BigDecimal("30.00"), CardAskPriceService.percentile(prices, 0.5));
        List<BigDecimal> even = List.of(new BigDecimal("10"), new BigDecimal("20"));
        assertEquals(new BigDecimal("15.00"), CardAskPriceService.percentile(even, 0.5));
        assertEquals(new BigDecimal("7.00"), CardAskPriceService.percentile(List.of(new BigDecimal("7")), 0.25));
    }

    @Test
    void summarizeKeepsUsdMatchesAndCheapestSamples() {
        Card c = card("Shohei Ohtani", null, false, null);
        AskStats stats = CardAskPriceService.summarize("q", c, List.of(
                listing("Ohtani RC", "40.00", "USD"),
                listing("Ohtani RC", "10.00", "USD"),
                listing("Ohtani RC", "30.00", "USD"),
                listing("Ohtani RC", "20.00", "USD"),
                listing("Ohtani RC", "50.00", "USD"),
                listing("Ohtani RC", "5.00", "GBP"),
                listing("Ohtani RC PSA 10", "500.00", "USD"),
                listing("Ohtani RC", "not a price", "USD")));
        assertEquals(5, stats.count());
        assertEquals(new BigDecimal("20.00"), stats.lowAsk());
        assertEquals(new BigDecimal("30.00"), stats.median());
        assertEquals(3, stats.samples().size());
        assertEquals(new BigDecimal("10.00"), stats.samples().get(0).price());
    }

    @Test
    void summarizeWithNoMatchesIsEmpty() {
        AskStats stats = CardAskPriceService.summarize("q", card("Shohei Ohtani", null, false, null), List.of());
        assertEquals(0, stats.count());
        assertNull(stats.lowAsk());
    }
}
