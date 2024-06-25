package RGcards.SportsCardProject.dao;

import RGcards.SportsCardProject.eto.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Integer> {

    @Query("SELECT c from Card c order by id desc limit 1")
    Card findLastCard();

    @Query("Select c from Card c where c.id = :id")
    Card findCardById(@Param("id") int id);

    @Query("SELECT c from Card c order by id asc")
    List<Card> findAllCardsSortByIdAsc();

    @Query("SELECT c from Card c order by id desc")
    List<Card> findAllCardsSortByIdDesc();

    @Query("SELECT c from Card c where year = :year")
    List<Card> findCardsByYear(@Param("year") String year);

    @Query(value = "select c.* from cards c inner join transaction_infos ti on ti.card_id = c.id where ti.move = 'out'",nativeQuery = true)
    List<Card> findSoldCards();


}
