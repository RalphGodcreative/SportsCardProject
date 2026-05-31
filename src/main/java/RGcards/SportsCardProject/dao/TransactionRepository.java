package RGcards.SportsCardProject.dao;

import RGcards.SportsCardProject.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId ORDER BY date DESC")
    List<Transaction> getTransactionsSortByDate(@Param("userId") Long userId);

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    List<Transaction> getTransactionsInDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
