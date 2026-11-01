package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dao.BowmanSearchHistoryRepository;
import RGcards.SportsCardProject.entity.BowmanSearchHistory;
import RGcards.SportsCardProject.enums.BowmanSearchType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BowmanSearchHistoryServiceTest {

    private final BowmanSearchHistoryRepository repository = mock(BowmanSearchHistoryRepository.class);
    private final BowmanSearchHistoryService service = new BowmanSearchHistoryService(repository);

    private BowmanSearchHistory saved() {
        ArgumentCaptor<BowmanSearchHistory> captor = ArgumentCaptor.forClass(BowmanSearchHistory.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void guestPlayerSearchIsSavedTrimmedWithNoUser() {
        service.recordPlayerSearch(null, "  Ohtani ");

        BowmanSearchHistory h = saved();
        assertEquals(BowmanSearchType.PLAYER, h.getSearchType());
        assertEquals("Ohtani", h.getPlayer());
        assertNull(h.getYear());
        assertNull(h.getSetName());
        assertNull(h.getUserId());
        assertNotNull(h.getCreatedAt());
    }

    @Test
    void playerSearchIsCutToMaxLength() {
        service.recordPlayerSearch(7L, "x".repeat(150));

        assertEquals(BowmanSearchHistoryService.MAX_PLAYER_LENGTH, saved().getPlayer().length());
    }

    @Test
    void setSearchKeepsUserAndTreatsBlankNameAsAny() {
        service.recordSetSearch(7L, (short) 2024, "  ");

        BowmanSearchHistory h = saved();
        assertEquals(BowmanSearchType.SET, h.getSearchType());
        assertNull(h.getPlayer());
        assertEquals((short) 2024, h.getYear());
        assertNull(h.getSetName());
        assertEquals(7L, h.getUserId());
    }

    @Test
    void saveFailureDoesNotPropagate() {
        when(repository.save(any())).thenThrow(new RuntimeException("db down"));

        service.recordSetSearch(null, null, "Bowman Chrome");
    }
}
