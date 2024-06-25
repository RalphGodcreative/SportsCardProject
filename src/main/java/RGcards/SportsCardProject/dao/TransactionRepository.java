package RGcards.SportsCardProject.dao;

import RGcards.SportsCardProject.eto.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    @Query("select t from Transaction t order by date desc")
    List<Transaction> getTransactionsSortByDate();

    @Query("select t from Transaction t where date >= :startDate and date <= :endDate order by date desc")
    List<Transaction> getTransactionsInDateRange(LocalDate startDate, LocalDate endDate);


}
