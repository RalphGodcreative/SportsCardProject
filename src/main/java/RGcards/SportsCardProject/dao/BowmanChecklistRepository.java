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
}
