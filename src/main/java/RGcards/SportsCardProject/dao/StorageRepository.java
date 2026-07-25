package RGcards.SportsCardProject.dao;

import RGcards.SportsCardProject.entity.Storage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StorageRepository extends JpaRepository<Storage, Long> {

    List<Storage> findByUserIdOrderByIdAsc(Long userId);

    Optional<Storage> findByIdAndUserId(long id, Long userId);
}
