package RGcards.SportsCardProject.dao;

import RGcards.SportsCardProject.entity.Recommendation;
import RGcards.SportsCardProject.enums.RecommendationAuthor;
import RGcards.SportsCardProject.enums.RecommendationSport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Integer> {

    List<Recommendation> findAllByOrderByCreatedAtDesc();

    Optional<Recommendation> findFirstBySportAndAuthorOrderByCreatedAtDesc(RecommendationSport sport, RecommendationAuthor author);
}
