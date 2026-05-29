package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.dto.TransactionWithCard;
import RGcards.SportsCardProject.entity.User;
import RGcards.SportsCardProject.service.CardService;
import RGcards.SportsCardProject.entity.Card;
import RGcards.SportsCardProject.entity.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RequestMapping("/transactions")
@Controller
public class TransactionController {

    @Autowired
    private CardService cardService;

    @GetMapping("/")
    public String allTransaction(Model model, @AuthenticationPrincipal User currentUser) {
        List<Transaction> transactions = cardService.findAllTransactionsSortByDate(currentUser.getId());
        model.addAttribute("transactions", transactions);
        return "allTransaction";
    }

    @GetMapping("/{transactionId}")
    public String showTransactionById(@PathVariable("transactionId") String transactionId, Model model,
                                      @AuthenticationPrincipal User currentUser) {
        int id = Integer.parseInt(transactionId);
        Transaction transaction = cardService.findTransactionById(id);
        if (transaction == null || !transaction.getUserId().equals(currentUser.getId())) {
            return "redirect:/transactions/";
        }
        List<Card> cards = cardService.findCardsByTransactionId(id);
        List<TransactionWithCard> transactionWithCards = new ArrayList<>();
        transactionWithCards.add(new TransactionWithCard(transaction, cards));
        model.addAttribute("transactionWithCardList", transactionWithCards);
        return "transactionList";
    }

    @DeleteMapping("/delete/{transactionId}")
    @ResponseBody
    public Boolean deleteTransaction(@PathVariable int transactionId, @AuthenticationPrincipal User currentUser) {
        Transaction transaction = cardService.findTransactionById(transactionId);
        if (transaction == null || !transaction.getUserId().equals(currentUser.getId())) {
            return false;
        }
        try {
            cardService.deleteTransactionAndAllRef(transactionId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
