package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.entity.Card;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellSignalAiServiceTest {

    @Test
    void parsesCardLinesAndIgnoresChatter() {
        String raw = """
                Here is my analysis:
                CARD 12: Hot streak, MVP odds rising.
                **CARD 7:** Injury news may cool demand.
                - CARD 3：Playoff run ends soon.
                Some closing remark.
                """;
        Map<Integer, String> r = SellSignalAiService.parse(raw, Set.of(12, 7, 3));
        assertEquals(3, r.size());
        assertEquals("Hot streak, MVP odds rising.", r.get(12));
        assertEquals("Injury news may cool demand.", r.get(7));
        assertEquals("Playoff run ends soon.", r.get(3));
    }

    @Test
    void ignoresIdsNotInThePromptAndDuplicates() {
        String raw = "CARD 99: not ours\nCARD 1: first\nCARD 1: second";
        Map<Integer, String> r = SellSignalAiService.parse(raw, Set.of(1));
        assertEquals(Map.of(1, "first"), r);
    }

    @Test
    void nullResponseGivesNoReasons() {
        assertTrue(SellSignalAiService.parse(null, Set.of(1)).isEmpty());
    }

    @Test
    void promptCarriesOurNumbersAndTheReplyFormat() {
        Card card = new Card();
        card.setId(42);
        card.setYear("2018");
        card.setPlayer("Shohei Ohtani");
        card.setGrade("PSA 10");
        String prompt = SellSignalAiService.buildPrompt(List.of(new SellSignalAiService.Candidate(
                card, new BigDecimal("1600"), new BigDecimal("100"), new BigDecimal("100.00"))));
        assertTrue(prompt.contains("CARD 42: 2018 Shohei Ohtani PSA 10"));
        assertTrue(prompt.contains("cost: 1600 TWD"));
        assertTrue(prompt.contains("listed from: 100.00 USD"));
        assertTrue(prompt.contains("CARD <id>: <reason>"));
        assertFalse(prompt.contains("null"));
    }
}
