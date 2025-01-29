package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.component.CardComponent;
import RGcards.SportsCardProject.eto.Card;
import RGcards.SportsCardProject.eto.SaleWithCard;
import RGcards.SportsCardProject.eto.Transaction;
import RGcards.SportsCardProject.eto.TransactionWithCard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/card")
@Controller
public class CardController {

    @Autowired
    private CardComponent component;


    @GetMapping("")
    public String main(Model model) {
        Card lastCard = component.getLastCard();
        model.addAttribute("lastCard", lastCard);
        return "cardMain";
    }

    @GetMapping("/addNewCard")
    public String addNewCard() {
        return "addCardPage";
    }

    @GetMapping("/saveCard")
    public String saveCard(@ModelAttribute("year") String year, @ModelAttribute("publisher") String publisher,
                           @ModelAttribute("set") String set, @ModelAttribute("player") String player,
                           @RequestParam(name = "auto", defaultValue = "false") Boolean auto, @ModelAttribute("insert") String insert,
                           @ModelAttribute("parallel") String parallel, @ModelAttribute("numbered") String numbered,
                           @ModelAttribute("sports") String sports, @ModelAttribute("grade") String grade,
                           @RequestParam(name = "value", defaultValue = "") Double value, @ModelAttribute("note") String note
    ) {
        Card card = new Card(year, publisher, set, player, auto, insert, parallel, numbered, sports, grade, value, note);
        int resultId = component.saveCard(card);
        System.out.println("the result id is " + resultId);

        return "redirect:/card/allCard";
    }

    @PostMapping("/updateCard")
    public String updateCard(@RequestParam(name = "id", defaultValue = "") String id, @ModelAttribute("year") String year, @ModelAttribute("publisher") String publisher,
                             @ModelAttribute("set") String set, @ModelAttribute("player") String player,
                             @RequestParam(name = "auto", defaultValue = "false") Boolean auto, @ModelAttribute("insert") String insert,
                             @ModelAttribute("parallel") String parallel, @ModelAttribute("numbered") String numbered,
                             @ModelAttribute("sports") String sports, @ModelAttribute("grade") String grade,
                             @RequestParam(name = "value", defaultValue = "") Double value, @ModelAttribute("note") String note
    ) {
        Card card = new Card(Integer.parseInt(id), year, publisher, set, player, auto, insert, parallel, numbered, sports, grade, value, note);
        System.out.println(card);
        int resultId = component.saveCard(card);
        System.out.println("the result id is " + resultId);

        return "redirect:/card/allCard";
    }


    @PostMapping("/saveTransaction")
    public String saveTransaction(@RequestBody TransactionWithCard transactionWithCard
    ) {

        component.saveTransactionWithCard(transactionWithCard);
        return "redirect:/card/allCard";
    }

    @PostMapping("/saveSale")
    public String saveSale(@RequestBody SaleWithCard saleWithCard) {
        component.saveSaleWithCard(saleWithCard);
        return "redirect:/card/allCard";
    }


    @GetMapping("/allCard")
    public String allCards(Model model) {
        List<Card> cards = component.getAllCardsSortById();
        int cardCounts = component.findCardsCount();
        model.addAttribute("cards", cards);
        model.addAttribute("cardCounts", cardCounts);

        return "allCard";
    }

    @GetMapping("/cards")
    public String allCardsByPage(Model model, @RequestParam(defaultValue = "1") int page) {
        List<Card> cards = component.findCardsByPage(page);
        int cardCounts = component.findCardsCount();
        model.addAttribute("cards", cards);
        model.addAttribute("cardCounts", cardCounts);
        model.addAttribute("page", page);
        return "pagingAllCard";
    }

    @GetMapping("/cardTransaction/{cardId}")
    public String getTransactionOfCard(Model model,@PathVariable String cardId){
        Transaction transaction = component.getTransactionByCardId(Integer.parseInt(cardId));
        return "redirect:/transactions/"+transaction.getId();
    }

    @GetMapping("/addTransaction")
    public String addTransaction(Model model) {
        return "addTransactionPage";
    }


    @GetMapping("/searchCard")
    public String searchCard(Model model) {
        return "searchCardPage";
    }

    @GetMapping("/sellTransaction")
    public String sellTransaction(Model model) {
        return "sellTransactionPage";
    }
}
