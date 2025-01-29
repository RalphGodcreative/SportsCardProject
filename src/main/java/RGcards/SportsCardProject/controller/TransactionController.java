package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.component.CardComponent;
import RGcards.SportsCardProject.eto.Card;
import RGcards.SportsCardProject.eto.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/transactions")
@Controller
public class TransactionController {

    @Autowired
    private CardComponent cardComponent;

    @GetMapping("/")
    public String allTransaction(Model model) {
        List<Transaction> transactions = cardComponent.findAllTransactionsSortByDate();
        model.addAttribute("transactions", transactions);

        return "allTransaction";
    }

    @GetMapping("/{transactionId}")
    public String showTransactionById(@PathVariable("transactionId") String transactionId, Model model) {
        Transaction transaction = cardComponent.findTransactionById(Integer.parseInt(transactionId));
        List<Card> cards = cardComponent.findCardsByTransactionId(Integer.parseInt(transactionId));
        model.addAttribute("transaction", transaction);
        model.addAttribute("cards",cards);

        return "transaction";
    }
    @PostMapping("/delete")
    @ResponseBody
    public Boolean deleteTransaction(@RequestParam(name = "transactionId") String transactionId) throws Exception {
        try{
            cardComponent.deleteTransactionAndAllRef(Integer.parseInt(transactionId));
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
