package RGcards.SportsCardProject.dao;

import RGcards.SportsCardProject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByGoogleSub(String googleSub);
    boolean existsByUsernameAndIdNot(String username, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);

    long countByRole(String role);

    @Modifying
    @Query("UPDATE User u SET u.aiCallCount = 0")
    void resetAllAiCallCounts();
}
