package RGcards.SportsCardProject.dao;

import RGcards.SportsCardProject.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByUserIdOrderByIdAsc(Long userId);

    Optional<Tag> findByIdAndUserId(long id, Long userId);

    /** Ownership-enforcing bulk lookup — the security boundary for tag assignment. */
    List<Tag> findByIdInAndUserId(Collection<Long> ids, Long userId);
}
