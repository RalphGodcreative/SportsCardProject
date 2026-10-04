package RGcards.SportsCardProject.dao;

import RGcards.SportsCardProject.entity.CardSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CardSetRepository extends JpaRepository<CardSet, Integer> {

    @Query("SELECT DISTINCT s.year FROM CardSet s WHERE s.year IS NOT NULL ORDER BY s.year DESC")
    List<Short> findDistinctYears();

    @Query("SELECT DISTINCT s.name FROM CardSet s WHERE s.name IS NOT NULL ORDER BY s.name")
    List<String> findDistinctNames();
}
