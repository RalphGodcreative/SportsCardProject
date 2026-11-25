package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dao.CardRepository;
import RGcards.SportsCardProject.dao.SellSignalRunRepository;
import RGcards.SportsCardProject.dto.AskStats;
import RGcards.SportsCardProject.dto.OwnedCardCost;
import RGcards.SportsCardProject.dto.SampleListing;
import RGcards.SportsCardProject.entity.Card;
import RGcards.SportsCardProject.entity.SellSignalItem;
import RGcards.SportsCardProject.entity.SellSignalRun;
import RGcards.SportsCardProject.entity.User;
import RGcards.SportsCardProject.enums.CardType;
import RGcards.SportsCardProject.enums.SellSignalAiStatus;
import RGcards.SportsCardProject.enums.SellSignalItemStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SellSignalServiceTest {

    private static final Long USER_ID = 7L;

    private final CardRepository cardRepository = mock(CardRepository.class);
    private final SellSignalRunRepository runRepository = mock(SellSignalRunRepository.class);
    private final CardAskPriceService askPriceService = mock(CardAskPriceService.class);
    private final SellSignalAiService aiService = mock(SellSignalAiService.class);
    private final AiCallGuard aiCallGuard = mock(AiCallGuard.class);
    private final AppSettingService appSettingService = mock(AppSettingService.class);

    private final SellSignalService service = new SellSignalService(cardRepository, runRepository,
            askPriceService, aiService, aiCallGuard, appSettingService, new ObjectMapper());

    private final List<OwnedCardCost> owned = new ArrayList<>();
    private final List<Card> cards = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(appSettingService.getUsdToTwd()).thenReturn(new BigDecimal("32"));
        when(cardRepository.findOwnedCardCosts(USER_ID)).thenReturn(owned);
        when(cardRepository.findByUserIdOrderByIdDesc(USER_ID)).thenReturn(cards);
        when(runRepository.save(any(SellSignalRun.class))).thenAnswer(inv -> inv.getArgument(0));
        when(runRepository.findFirstByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(Optional.empty());
        when(aiCallGuard.hasRemaining(any())).thenReturn(true);
    }

    private static User user(String role) {
        User u = new User();
        u.setId(USER_ID);
        u.setRole(role);
        return u;
    }

    private Card ownedCard(int id, CardType type, String cost) {
        Card c = new Card();
        c.setId(id);
        c.setType(type);
        c.setPlayer("Player " + id);
        c.setUserId(USER_ID);
        cards.add(c);
        BigDecimal costValue = cost == null ? null : new BigDecimal(cost);
        owned.add(new OwnedCardCost() {
            public Integer getCardId() { return id; }
            public BigDecimal getCostBasis() { return costValue; }
        });
        return c;
    }

    private static AskStats asks(int count, String lowAsk) {
        BigDecimal low = new BigDecimal(lowAsk);
        return new AskStats("q", count, low, low, List.of(new SampleListing("t", low, "u")));
    }

    private static SellSignalItem item(SellSignalRun run, int cardId) {
        return run.getItems().stream().filter(i -> i.getCardId() == cardId).findFirst().orElseThrow();
    }

    @Test
    void ranksByScoreAndExplainsTopCards() throws Exception {
        Card a = ownedCard(1, CardType.INVESTMENT, "1600");   // +100%
        Card b = ownedCard(2, CardType.NEUTRAL, "1600");      // +300%
        Card c = ownedCard(3, CardType.NEUTRAL, null);        // no cost
        when(askPriceService.lookup(a)).thenReturn(asks(5, "100"));
        when(askPriceService.lookup(b)).thenReturn(asks(5, "200"));
        when(askPriceService.lookup(c)).thenReturn(asks(5, "50"));
        when(aiService.explain(anyList())).thenReturn(Map.of(2, "Hot streak"));

        SellSignalService.RunOutcome outcome = service.run(user("ROLE_ADMIN"));

        assertTrue(outcome.started());
        SellSignalRun run = outcome.run();
        assertEquals(3, run.getCardsConsidered());
        assertEquals(SellSignalAiStatus.OK, run.getAiStatus());
        assertEquals((short) 1, item(run, 2).getRank());
        assertEquals((short) 2, item(run, 1).getRank());
        assertEquals("Hot streak", item(run, 2).getAiReason());
        assertEquals(SellSignalItemStatus.NO_COST, item(run, 3).getStatus());
        assertNull(item(run, 3).getRank());
        assertTrue(item(run, 2).getSampleListings().contains("\"price\""));
        verify(aiCallGuard).recordCall(any());
    }

    @Test
    void ebayFailureOnOneCardDoesNotStopTheRun() {
        Card a = ownedCard(1, CardType.NEUTRAL, "1600");
        Card b = ownedCard(2, CardType.NEUTRAL, "1600");
        when(askPriceService.lookup(a)).thenThrow(new RestClientException("boom"));
        when(askPriceService.lookup(b)).thenReturn(asks(5, "100"));

        SellSignalRun run = service.run(user("ROLE_ADMIN")).run();

        assertEquals(SellSignalItemStatus.EBAY_ERROR, item(run, 1).getStatus());
        assertEquals(SellSignalItemStatus.RANKED, item(run, 2).getStatus());
    }

    @Test
    void aiFailureStillSavesTheRankingWithoutCountingACall() throws Exception {
        Card a = ownedCard(1, CardType.NEUTRAL, "1600");
        when(askPriceService.lookup(a)).thenReturn(asks(5, "100"));
        when(aiService.explain(anyList())).thenThrow(new RestClientException("gemini down"));

        SellSignalRun run = service.run(user("ROLE_ADMIN")).run();

        assertEquals(SellSignalAiStatus.FAILED, run.getAiStatus());
        assertEquals((short) 1, item(run, 1).getRank());
        verify(aiCallGuard, never()).recordCall(any());
    }

    @Test
    void aiIsSkippedAtTheUsageLimit() throws Exception {
        Card a = ownedCard(1, CardType.NEUTRAL, "1600");
        when(askPriceService.lookup(a)).thenReturn(asks(5, "100"));
        when(aiCallGuard.hasRemaining(any())).thenReturn(false);

        SellSignalRun run = service.run(user("ROLE_USER")).run();

        assertEquals(SellSignalAiStatus.SKIPPED, run.getAiStatus());
        verify(aiService, never()).explain(anyList());
    }

    @Test
    void aiIsSkippedWhenNoCardIsAboveCost() throws Exception {
        Card a = ownedCard(1, CardType.NEUTRAL, "6400");   // 100 USD * 32 = 3200 TWD: -50%
        when(askPriceService.lookup(a)).thenReturn(asks(5, "100"));

        SellSignalRun run = service.run(user("ROLE_ADMIN")).run();

        assertEquals((short) 1, item(run, 1).getRank());
        assertEquals(SellSignalAiStatus.SKIPPED, run.getAiStatus());
        verify(aiService, never()).explain(anyList());
    }

    @Test
    void cooldownBlocksNonAdminsButNotAdmins() {
        SellSignalRun recent = new SellSignalRun();
        recent.setCreatedAt(LocalDateTime.now().minusHours(1));
        when(runRepository.findFirstByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(Optional.of(recent));

        SellSignalService.RunOutcome blocked = service.run(user("ROLE_USER"));
        assertFalse(blocked.started());
        assertSame(recent, blocked.run());

        assertTrue(service.run(user("ROLE_ADMIN")).started());
    }

    @Test
    void cooldownEndsAfterTwentyFourHours() {
        SellSignalRun old = new SellSignalRun();
        old.setCreatedAt(LocalDateTime.now().minusHours(25));
        assertNull(SellSignalService.nextRunAt(user("ROLE_USER"), old, LocalDateTime.now()));
    }

    @Test
    void selectsInvestmentCardsFirstThenHighestCostAndCapsAtTwenty() {
        for (int i = 1; i <= 25; i++) {
            Card c = ownedCard(i, CardType.NEUTRAL, String.valueOf(i * 100));
            when(askPriceService.lookup(c)).thenReturn(asks(5, "100"));
        }
        Card investment = ownedCard(100, CardType.INVESTMENT, "1");
        when(askPriceService.lookup(investment)).thenReturn(asks(5, "100"));

        SellSignalRun run = service.run(user("ROLE_ADMIN")).run();

        assertEquals(SellSignalService.MAX_CARDS_PER_RUN, run.getItems().size());
        assertEquals(100, run.getItems().get(0).getCardId());
        assertEquals(25, run.getItems().get(1).getCardId());
        assertTrue(run.getItems().stream().noneMatch(i -> i.getCardId() <= 6));
    }

    @Test
    void cardsNotInTheOwnedListAreNeverScanned() {
        Card sold = new Card();
        sold.setId(50);
        sold.setUserId(USER_ID);
        cards.add(sold);

        SellSignalRun run = service.run(user("ROLE_ADMIN")).run();

        assertEquals(0, run.getCardsConsidered());
        verify(askPriceService, never()).lookup(sold);
    }
}
