package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dao.BowmanChecklistRepository;
import RGcards.SportsCardProject.dao.CardSetRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BowmanChecklistSearchServiceTest {

    private final BowmanChecklistRepository repository = mock(BowmanChecklistRepository.class);
    private final CardSetRepository setRepository = mock(CardSetRepository.class);
    private final BowmanChecklistSearchService service =
            new BowmanChecklistSearchService(repository, setRepository);

    @Test
    void playerSearchTrimsInput() {
        service.searchByPlayer("  Skenes ");

        verify(repository).searchByPlayer("Skenes");
    }

    @Test
    void setSearchTreatsNullYearAndBlankNameAsAny() {
        service.searchBySet(null, "  ");

        verify(repository).searchBySet((short) 0, "");
    }

    @Test
    void setSearchPassesYearAndNameThrough() {
        service.searchBySet((short) 2025, "Bowman Chrome");

        verify(repository).searchBySet((short) 2025, "Bowman Chrome");
    }
}
