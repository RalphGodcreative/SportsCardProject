package RGcards.SportsCardProject.dao;

import RGcards.SportsCardProject.dto.OwnedCardCost;
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

    List<Card> findByTags_IdAndUserIdOrderByIdDesc(Long tagId, Long userId);

    long countByTags_IdAndUserId(Long tagId, Long userId);

    @Query("SELECT c FROM Card c WHERE c.userId = :userId AND c.year LIKE %:year%")
    List<Card> findCardsByYearAndUserId(@Param("userId") Long userId, @Param("year") String year);

    @Query(value = "SELECT c.* FROM cards c INNER JOIN transaction_infos ti ON ti.card_id = c.id WHERE ti.move = 'OUT' AND c.user_id = :userId", nativeQuery = true)
    List<Card> findSoldCards(@Param("userId") Long userId);

    @Query(value = "SELECT c.* FROM cards c INNER JOIN transaction_infos ti ON ti.card_id = c.id WHERE ti.transaction_id = :transactionId ORDER BY c.id DESC", nativeQuery = true)
    List<Card> findCardsByTransactionId(@Param("transactionId") int transactionId);

    /**
     * Cards the user still owns (no OUT move), excluding PC cards, with a cost basis in TWD:
     * the IN transaction's amount split evenly across the cards bought in it. Cost is null when
     * the card has no IN transaction. A card with several IN rows (data error) appears once per row.
     */
    @Query(value = """
            SELECT c.id AS "cardId",
                   CAST(t.amount / NULLIF(cnt.n, 0) AS numeric) AS "costBasis"
            FROM cards c
            LEFT JOIN transaction_infos ti_in ON ti_in.card_id = c.id AND ti_in.move = 'IN'
            LEFT JOIN transactions t ON t.id = ti_in.transaction_id AND t.user_id = :userId
            LEFT JOIN (SELECT transaction_id, COUNT(*) AS n
                       FROM transaction_infos WHERE move = 'IN'
                       GROUP BY transaction_id) cnt ON cnt.transaction_id = ti_in.transaction_id
            WHERE c.user_id = :userId
              AND (c.type IS NULL OR c.type <> 'PC')
              AND NOT EXISTS (SELECT 1 FROM transaction_infos o WHERE o.card_id = c.id AND o.move = 'OUT')
            """, nativeQuery = true)
    List<OwnedCardCost> findOwnedCardCosts(@Param("userId") Long userId);
}
