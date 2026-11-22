package RGcards.SportsCardProject.dao;

import RGcards.SportsCardProject.entity.CardSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CardSetRepository extends JpaRepository<CardSet, Integer> {

    // The sets table holds every Topps/Panini product, but only some have 1st Bowman
    // checklist rows; these feed the Bowman search dropdowns, so scope them to those.
    @Query("SELECT DISTINCT s.year FROM CardSet s WHERE s.year IS NOT NULL " +
            "AND EXISTS (SELECT 1 FROM BowmanChecklist bc WHERE bc.set = s) ORDER BY s.year DESC")
    List<Short> findDistinctYears();

    @Query("SELECT DISTINCT s.name FROM CardSet s WHERE s.name IS NOT NULL " +
            "AND EXISTS (SELECT 1 FROM BowmanChecklist bc WHERE bc.set = s) ORDER BY s.name")
    List<String> findDistinctNames();
}
