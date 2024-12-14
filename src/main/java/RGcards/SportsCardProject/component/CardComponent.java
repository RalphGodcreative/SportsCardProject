package RGcards.SportsCardProject.component;

import RGcards.SportsCardProject.dao.CardDao;
import RGcards.SportsCardProject.dao.CardRepository;
import RGcards.SportsCardProject.dao.TransactionInfoRepository;
import RGcards.SportsCardProject.dao.TransactionRepository;
import RGcards.SportsCardProject.eto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
public class CardComponent {

    @Autowired
    private CardRepository cardRepo;
    @Autowired
    private TransactionRepository tranRepo;
    @Autowired
    private TransactionInfoRepository tranInfoRepo;

    @Autowired
    private CardDao cardDao;


    public List<Card> getAllCards() {
        List<Card> cards = cardRepo.findAll();
        return cards;
    }

    public List<Card> getAllCardsSortById() {
        List<Card> cards = cardRepo.findAllCardsSortByIdDesc();
        return cards;
    }

    public List<Card> getCardsByYear(String year) {
        List<Card> cards = cardRepo.findCardsByYear(year);
        return cards;
    }

    public List<Card> findCardsWithParam(Card card) {
        List<Card> cards = cardDao.getCardByParams(card);
        return cards;
    }

    public List<Card> findCardsByPage(int page) {
        List<Card> cards = cardDao.getCardsByPage(page);
        return cards;
    }

    public List<Card> getSoldCards() {
        List<Card> cards = cardRepo.findSoldCards();
        return cards;
    }

    public int findCardsCount() {
        int res = (int) cardRepo.count();
        System.out.println("cards : " + res);
        return res;
    }

    public Card getLastCard() {
        Card card = cardRepo.findLastCard();
        return card;
    }

    public Card getCardById(int id) {
        Card card = cardRepo.findCardById(id);
        return card;
    }

    public int saveCard(Card card) {
        int id = cardRepo.save(card).getId();
        return id;
    }

    public int saveTransaction(Transaction transaction) {
        int id = tranRepo.save(transaction).getId();
        return id;
    }

    public int saveTransactionInfo(TransactionInfo transactionInfo) {
        int id = tranInfoRepo.save(transactionInfo).getId();
        return id;
    }

    public void saveTransactionWithCard(TransactionWithCard transactionWithCard) {
        Transaction transaction = transactionWithCard.getTransaction();
        List<Card> cards = transactionWithCard.getCards();
        int transactionId = saveTransaction(transaction);
        for (Card card : cards) {
            int cardId = saveCard(card);
            TransactionInfo ti = new TransactionInfo(transactionId, cardId, "in");
            int result = saveTransactionInfo(ti);
        }

    }

    public void saveSaleWithCard(SaleWithCard saleWithCard) {
        Transaction transaction = saleWithCard.getTransaction();
        List<Integer> cardIds = saleWithCard.getCardIds();
        int transactionId = saveTransaction(transaction);
        for (Integer cardId : cardIds) {
            TransactionInfo ti = new TransactionInfo(transactionId, cardId, "out");
            int result = saveTransactionInfo(ti);
        }

    }

    public void deleteTransactionAndAllRef(int transactionId) {
        List<TransactionInfo> transactionInfos = findTransactionInfoByTransactionId(transactionId);
        List<Card> cards = findCardsByTransactionId(transactionId);
        if(!transactionInfos.isEmpty()){
            tranInfoRepo.deleteAll(transactionInfos);
        }
        if(!cards.isEmpty()){
            cardRepo.deleteAll(cards);
        }
        tranRepo.deleteById(transactionId);
    }

    public int deleteCard(int cardId){
        int deleteCount = 0 ;
        if(!cardRepo.existsById(cardId)) return deleteCount;
        TransactionInfo transactionInfo = tranInfoRepo.findByCardId(cardId);
        if(transactionInfo != null) tranInfoRepo.deleteById(transactionInfo.getId());
        cardRepo.deleteById(cardId);
        deleteCount++;
        return deleteCount;
    }

    public void updateCard() {
        Card card = cardRepo.findCardById(1);
        card.setValue(330.00);
        cardRepo.save(card);
        System.out.println(cardRepo.findCardById(1));
    }

    public List<Transaction> findAllTransactionsSortByDate() {
        List<Transaction> transactions = tranRepo.getTransactionsSortByDate();
        return transactions;
    }

    public List<Transaction> findTransactionsInDateRange(LocalDate startDate, LocalDate endDate) {
        List<Transaction> transactions = tranRepo.getTransactionsInDateRange(startDate, endDate);
        return transactions;
    }

    public Transaction findTransactionById(int id) {
        Transaction transaction = tranRepo.findById(id).isPresent() ? tranRepo.findById(id).get() : null;
        return transaction;
    }

    public List<TransactionInfo> findTransactionInfoByTransactionId(int transactionId) {
        List<TransactionInfo> transactionInfos = tranInfoRepo.getTransactionInfosByTransactionId(transactionId);
        return transactionInfos;
    }

    public List<Card> findCardsByTransactionId(int transactionId) {
        List<Card> cards = cardDao.getCardsByTransactionId(transactionId);
        return cards;
    }

    public Transaction getTransactionByCardId(int cardId){

        try {
            TransactionInfo ti = tranInfoRepo.findByCardId(cardId);
            Transaction transaction = tranRepo.findById(ti.getTransactionId()).get();
            return transaction;
        }catch (Exception e){
            log.error(e.getMessage());
        }
        return null;
    }

}
