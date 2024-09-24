package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.component.CardComponent;
import RGcards.SportsCardProject.eto.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequestMapping("/transaction")
@Controller
public class TransactionController {

    @Autowired
    private CardComponent cardComponent;

    @GetMapping("/")
    public String allTransaction(Model model){
        List<Transaction> transactions = cardComponent.findAllTransactionsSortByDate();
        model.addAttribute("transactions" , transactions);

        return "allTransaction";
    }
}
