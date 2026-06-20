package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dao.CardRepository;
import RGcards.SportsCardProject.dao.CardSpec;
import RGcards.SportsCardProject.dao.TransactionInfoRepository;
import RGcards.SportsCardProject.dao.TransactionRepository;
import RGcards.SportsCardProject.dto.TransactionWithCard;
import RGcards.SportsCardProject.entity.Card;
import RGcards.SportsCardProject.entity.SaleWithCard;
import RGcards.SportsCardProject.entity.Transaction;
import RGcards.SportsCardProject.entity.TransactionInfo;
import RGcards.SportsCardProject.enums.MoveType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CardService {

    @Autowired
    private CardRepository cardRepo;
    @Autowired
    private TransactionRepository tranRepo;
    @Autowired
    private TransactionInfoRepository tranInfoRepo;

    public List<Card> getAllCardsSortById(Long userId) {
        return cardRepo.findByUserIdOrderByIdDesc(userId);
    }

    public List<Card> getCardsByYear(String year, Long userId) {
        return cardRepo.findCardsByYearAndUserId(userId, year);
    }

    public List<Card> findCardsWithParam(Card card, Long userId) {
        return cardRepo.findAll(CardSpec.build(card, userId), Sort.by(Sort.Direction.DESC, "id"));
    }

    public List<Card> findCardsByPage(int page, Long userId) {
        PageRequest pageRequest = PageRequest.of(page - 1, 10, Sort.by(Sort.Direction.DESC, "id"));
        return cardRepo.findAll(CardSpec.forUser(userId), pageRequest).getContent();
    }

    public List<Card> getSoldCards(Long userId) {
        return cardRepo.findSoldCards(userId);
    }

    public int findCardsCount(Long userId) {
        int res = (int) cardRepo.countByUserId(userId);
        log.info("cards: " + res);
        return res;
    }

    public Card getLastCard(Long userId) {
        return cardRepo.findFirstByUserIdOrderByIdDesc(userId);
    }

    public Card getCardById(int id, Long userId) {
        return cardRepo.findByIdAndUserId(id, userId).orElse(null);
    }

    public int saveCard(Card card) {
        return cardRepo.save(card).getId();
    }

    public int saveTransaction(Transaction transaction) {
        return tranRepo.save(transaction).getId();
    }

    public int saveTransactionInfo(TransactionInfo transactionInfo) {
        return tranInfoRepo.save(transactionInfo).getId();
    }

    public void saveTransactionWithCard(TransactionWithCard transactionWithCard, Long userId) {
        Transaction transaction = transactionWithCard.getTransaction();
        transaction.setUserId(userId);
        List<Card> cards = transactionWithCard.getCards();
        int transactionId = saveTransaction(transaction);
        for (Card card : cards) {
            card.setUserId(userId);
            int cardId = saveCard(card);
            saveTransactionInfo(new TransactionInfo(transactionId, cardId, MoveType.IN));
        }
    }

    public void addCardsToTransaction(int transactionId, List<Card> cards, Long userId) {
        for (Card card : cards) {
            card.setUserId(userId);
            int cardId = saveCard(card);
            saveTransactionInfo(new TransactionInfo(transactionId, cardId, MoveType.IN));
        }
    }

    public void saveSaleWithCard(SaleWithCard saleWithCard, Long userId) {
        Transaction transaction = saleWithCard.getTransaction();
        transaction.setUserId(userId);
        List<Integer> cardIds = saleWithCard.getCardIds();
        int transactionId = saveTransaction(transaction);
        for (Integer cardId : cardIds) {
            saveTransactionInfo(new TransactionInfo(transactionId, cardId, MoveType.OUT));
        }
    }

    public void deleteTransactionAndAllRef(int transactionId) {
        List<TransactionInfo> transactionInfos = findTransactionInfoByTransactionId(transactionId);
        List<Card> cards = findCardsByTransactionId(transactionId);
        if (!transactionInfos.isEmpty()) {
            tranInfoRepo.deleteAll(transactionInfos);
        }
        if (!cards.isEmpty()) {
            cardRepo.deleteAll(cards);
        }
        tranRepo.deleteById(transactionId);
    }

    public int deleteCard(int cardId) {
        int deleteCount = 0;
        if (!cardRepo.existsById(cardId)) return deleteCount;
        List<TransactionInfo> transactionInfos = tranInfoRepo.findByCardId(cardId);
        if (!transactionInfos.isEmpty()) {
            for (TransactionInfo transactionInfo : transactionInfos) {
                tranInfoRepo.deleteById(transactionInfo.getId());
            }
        }
        cardRepo.deleteById(cardId);
        deleteCount++;
        return deleteCount;
    }

    public List<Transaction> findAllTransactionsSortByDate(Long userId) {
        return tranRepo.getTransactionsSortByDate(userId);
    }

    public List<Transaction> findTransactionsInDateRange(LocalDate startDate, LocalDate endDate, Long userId) {
        return tranRepo.getTransactionsInDateRange(userId, startDate, endDate);
    }

    public Transaction findTransactionById(int id) {
        return tranRepo.findById(id).orElse(null);
    }

    public List<TransactionInfo> findTransactionInfoByTransactionId(int transactionId) {
        return tranInfoRepo.findByTransactionId(transactionId);
    }

    public List<Card> findCardsByTransactionId(int transactionId) {
        return cardRepo.findCardsByTransactionId(transactionId);
    }

    public List<Transaction> getTransactionByCardId(int cardId) {
        try {
            List<TransactionInfo> tis = tranInfoRepo.findByCardId(cardId);
            List<Transaction> transactions = new ArrayList<>();
            for (TransactionInfo ti : tis) {
                tranRepo.findById(ti.getTransactionId()).ifPresent(transactions::add);
            }
            return transactions;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }
}
