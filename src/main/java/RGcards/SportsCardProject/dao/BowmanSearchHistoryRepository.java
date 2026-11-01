package RGcards.SportsCardProject.dao;

import RGcards.SportsCardProject.dto.AdminBowmanSearchRow;
import RGcards.SportsCardProject.dto.BowmanPlayerSearchCount;
import RGcards.SportsCardProject.dto.BowmanSetSearchCount;
import RGcards.SportsCardProject.entity.BowmanSearchHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BowmanSearchHistoryRepository extends JpaRepository<BowmanSearchHistory, Long> {

    /** Latest searches with the username (null = guest or deleted user). */
    @Query("""
            SELECT new RGcards.SportsCardProject.dto.AdminBowmanSearchRow(
                       h.id, h.searchType, h.player, h.year, h.setName, u.username, h.createdAt)
            FROM BowmanSearchHistory h LEFT JOIN User u ON u.id = h.userId
            ORDER BY h.createdAt DESC
            """)
    List<AdminBowmanSearchRow> findLatest(Pageable pageable);

    /** Most-searched players since a date, case-insensitive. */
    @Query("""
            SELECT new RGcards.SportsCardProject.dto.BowmanPlayerSearchCount(LOWER(h.player), COUNT(h))
            FROM BowmanSearchHistory h
            WHERE h.searchType = RGcards.SportsCardProject.enums.BowmanSearchType.PLAYER
              AND h.createdAt >= :since
            GROUP BY LOWER(h.player)
            ORDER BY COUNT(h) DESC
            """)
    List<BowmanPlayerSearchCount> findTopPlayers(@Param("since") LocalDateTime since, Pageable pageable);

    /** Most-searched sets since a date; a null year or set name means "any" was picked. */
    @Query("""
            SELECT new RGcards.SportsCardProject.dto.BowmanSetSearchCount(h.year, h.setName, COUNT(h))
            FROM BowmanSearchHistory h
            WHERE h.searchType = RGcards.SportsCardProject.enums.BowmanSearchType.SET
              AND h.createdAt >= :since
            GROUP BY h.year, h.setName
            ORDER BY COUNT(h) DESC
            """)
    List<BowmanSetSearchCount> findTopSets(@Param("since") LocalDateTime since, Pageable pageable);
}
