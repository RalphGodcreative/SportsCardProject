package RGcards.SportsCardProject.dao;

import RGcards.SportsCardProject.entity.TransactionInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TransactionInfoRepository extends JpaRepository<TransactionInfo,Integer> {

    List<TransactionInfo> findByTransactionId(int transactionId);

    List<TransactionInfo> findByCardId(int cardId);
}
