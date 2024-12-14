package RGcards.SportsCardProject.dao;

import RGcards.SportsCardProject.eto.TransactionInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TransactionInfoRepository extends JpaRepository<TransactionInfo,Integer> {

    @Query("select ti from TransactionInfo ti where transactionId = :transactionId")
    List<TransactionInfo> getTransactionInfosByTransactionId(int transactionId);

    TransactionInfo findByCardId(int cardId);
}
