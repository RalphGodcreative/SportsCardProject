package RGcards.SportsCardProject.dao;

import RGcards.SportsCardProject.entity.BowmanChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BowmanChecklistRepository extends JpaRepository<BowmanChecklist, Long> {

    @Query("SELECT bc FROM BowmanChecklist bc JOIN FETCH bc.set")
    List<BowmanChecklist> findAllWithSet();

    @Query("SELECT bc FROM BowmanChecklist bc JOIN FETCH bc.set WHERE bc.set.id = :setId")
    List<BowmanChecklist> findBySetId(@Param("setId") Integer setId);

    @Query("SELECT bc FROM BowmanChecklist bc JOIN FETCH bc.set s " +
            "WHERE LOWER(COALESCE(bc.player, '')) LIKE LOWER(CONCAT('%', :player, '%')) " +
            "ORDER BY s.year DESC, s.name, bc.player")
    List<BowmanChecklist> searchByPlayer(@Param("player") String player);

    // Year 0 / blank setName mean "any" (avoids typed-null parameters, which some
    // databases cannot infer a type for).
    @Query("SELECT bc FROM BowmanChecklist bc JOIN FETCH bc.set s " +
            "WHERE (:year = 0 OR s.year = :year) AND (:setName = '' OR s.name = :setName) " +
            "ORDER BY s.year DESC, s.name, bc.player")
    List<BowmanChecklist> searchBySet(@Param("year") Short year, @Param("setName") String setName);
}
