package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dao.BowmanSearchHistoryRepository;
import RGcards.SportsCardProject.dto.AdminBowmanSearchRow;
import RGcards.SportsCardProject.dto.BowmanPlayerSearchCount;
import RGcards.SportsCardProject.dto.BowmanSetSearchCount;
import RGcards.SportsCardProject.entity.BowmanSearchHistory;
import RGcards.SportsCardProject.enums.BowmanSearchType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BowmanSearchHistoryService {

    static final int MAX_PLAYER_LENGTH = 100;

    private final BowmanSearchHistoryRepository repository;

    /** userId is null for guests. */
    public void recordPlayerSearch(Long userId, String player) {
        String cleaned = blankToNull(player);
        if (cleaned != null && cleaned.length() > MAX_PLAYER_LENGTH) {
            cleaned = cleaned.substring(0, MAX_PLAYER_LENGTH);
        }
        save(BowmanSearchType.PLAYER, cleaned, null, null, userId);
    }

    /** userId is null for guests; a null year or blank set name means "any". */
    public void recordSetSearch(Long userId, Short year, String setName) {
        save(BowmanSearchType.SET, null, year, blankToNull(setName), userId);
    }

    public List<AdminBowmanSearchRow> findLatest(int limit) {
        return repository.findLatest(PageRequest.of(0, limit));
    }

    public List<BowmanPlayerSearchCount> findTopPlayers(LocalDateTime since, int limit) {
        return repository.findTopPlayers(since, PageRequest.of(0, limit));
    }

    public List<BowmanSetSearchCount> findTopSets(LocalDateTime since, int limit) {
        return repository.findTopSets(since, PageRequest.of(0, limit));
    }

    private void save(BowmanSearchType type, String player, Short year, String setName, Long userId) {
        try {
            BowmanSearchHistory history = new BowmanSearchHistory();
            history.setSearchType(type);
            history.setPlayer(player);
            history.setYear(year);
            history.setSetName(setName);
            history.setUserId(userId);
            history.setCreatedAt(LocalDateTime.now());
            repository.save(history);
        } catch (Exception e) {
            // History is best-effort: never fail the user's search because of it.
            log.warn("Failed to record Bowman search history", e);
        }
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
