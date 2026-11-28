package RGcards.SportsCardProject.dao;

import RGcards.SportsCardProject.entity.SellSignalRun;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Always scoped by user: never add an unscoped finder here. */
public interface SellSignalRunRepository extends JpaRepository<SellSignalRun, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<SellSignalRun> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
}
