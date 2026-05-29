package RGcards.SportsCardProject.dao;

import RGcards.SportsCardProject.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Integer>, JpaSpecificationExecutor<Card> {

    Card findFirstByUserIdOrderByIdDesc(Long userId);

    Optional<Card> findByIdAndUserId(int id, Long userId);

    List<Card> findByUserIdOrderByIdAsc(Long userId);

    List<Card> findByUserIdOrderByIdDesc(Long userId);

    long countByUserId(Long userId);

    @Query("SELECT c FROM Card c WHERE c.userId = :userId AND c.year LIKE %:year%")
    List<Card> findCardsByYearAndUserId(@Param("userId") Long userId, @Param("year") String year);

    @Query(value = "SELECT c.* FROM cards c INNER JOIN transaction_infos ti ON ti.card_id = c.id WHERE ti.move = 'OUT' AND c.user_id = :userId", nativeQuery = true)
    List<Card> findSoldCards(@Param("userId") Long userId);

    @Query(value = "SELECT c.* FROM cards c INNER JOIN transaction_infos ti ON ti.card_id = c.id WHERE ti.transaction_id = :transactionId ORDER BY c.id DESC", nativeQuery = true)
    List<Card> findCardsByTransactionId(@Param("transactionId") int transactionId);
}
