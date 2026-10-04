package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dao.BowmanChecklistRepository;
import RGcards.SportsCardProject.dao.CardSetRepository;
import RGcards.SportsCardProject.entity.BowmanChecklist;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BowmanChecklistSearchService {

    private final BowmanChecklistRepository bowmanChecklistRepository;
    private final CardSetRepository cardSetRepository;

    /** Partial, case-insensitive match on the player name. */
    public List<BowmanChecklist> searchByPlayer(String player) {
        return bowmanChecklistRepository.searchByPlayer(clean(player));
    }

    /** Exact match on year and/or set name; a null year or blank name means "any". */
    public List<BowmanChecklist> searchBySet(Short year, String setName) {
        return bowmanChecklistRepository.searchBySet(
                year == null ? Short.valueOf((short) 0) : year, clean(setName));
    }

    public List<Short> findYears() {
        return cardSetRepository.findDistinctYears();
    }

    public List<String> findSetNames() {
        return cardSetRepository.findDistinctNames();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
