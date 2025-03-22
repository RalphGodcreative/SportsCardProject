package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.service.CardService;
import RGcards.SportsCardProject.entity.Card;
import RGcards.SportsCardProject.entity.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/transactions")
@Controller
public class TransactionController {

    @Autowired
    private CardService cardService;

    @GetMapping("/")
    public String allTransaction(Model model) {
        List<Transaction> transactions = cardService.findAllTransactionsSortByDate();
        model.addAttribute("transactions", transactions);

        return "allTransaction";
    }

    @GetMapping("/{transactionId}")
    public String showTransactionById(@PathVariable("transactionId") String transactionId, Model model) {
        Transaction transaction = cardService.findTransactionById(Integer.parseInt(transactionId));
        List<Card> cards = cardService.findCardsByTransactionId(Integer.parseInt(transactionId));
        model.addAttribute("transaction", transaction);
        model.addAttribute("cards",cards);

        return "transaction";
    }
    @PostMapping("/delete")
    @ResponseBody
    public Boolean deleteTransaction(@RequestParam(name = "transactionId") String transactionId) throws Exception {
        try{
            cardService.deleteTransactionAndAllRef(Integer.parseInt(transactionId));
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
