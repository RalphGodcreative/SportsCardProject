package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.service.CardService;
import RGcards.SportsCardProject.entity.Card;
import RGcards.SportsCardProject.entity.SaleWithCard;
import RGcards.SportsCardProject.entity.Transaction;
import RGcards.SportsCardProject.dto.TransactionWithCard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RequestMapping("/card")
@Controller
public class CardController {

    @Autowired
    private CardService cardService;


    @GetMapping("")
    public String main(Model model) {
        Card lastCard = cardService.getLastCard();
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
        int resultId = cardService.saveCard(card);
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
        int resultId = cardService.saveCard(card);
        System.out.println("the result id is " + resultId);

        return "redirect:/card/allCard";
    }


    @PostMapping("/saveTransaction")
    public String saveTransaction(@RequestBody TransactionWithCard transactionWithCard
    ) {

        cardService.saveTransactionWithCard(transactionWithCard);
        return "redirect:/card/allCard";
    }

    @PostMapping("/saveSale")
    public String saveSale(@RequestBody SaleWithCard saleWithCard) {
        cardService.saveSaleWithCard(saleWithCard);
        return "redirect:/card/allCard";
    }


    @GetMapping("/allCard")
    public String allCards(Model model) {
        List<Card> cards = cardService.getAllCardsSortById();
        int cardCounts = cardService.findCardsCount();
        model.addAttribute("cards", cards);
        model.addAttribute("cardCounts", cardCounts);

        return "allCard";
    }

    @GetMapping("/cards")
    public String allCardsByPage(Model model, @RequestParam(defaultValue = "1") int page) {
        List<Card> cards = cardService.findCardsByPage(page);
        int cardCounts = cardService.findCardsCount();
        model.addAttribute("cards", cards);
        model.addAttribute("cardCounts", cardCounts);
        model.addAttribute("page", page);
        return "pagingAllCard";
    }

    @GetMapping("/cardTransaction/{cardId}")
    public String getTransactionOfCard(Model model,@PathVariable String cardId){
        List<Transaction> transactions = cardService.getTransactionByCardId(Integer.parseInt(cardId));
        if(transactions.size()==1){
            Transaction transaction = transactions.get(0);
            return "redirect:/transactions/"+transaction.getId();
        }else if(transactions.size() > 1){
            List<TransactionWithCard> transactionWithCards = new ArrayList<>();
            for(Transaction transaction:transactions){
                List<Card> cards = cardService.findCardsByTransactionId(transaction.getId());
                TransactionWithCard transactionWithCard = new TransactionWithCard(transaction, cards);
                transactionWithCards.add(transactionWithCard);
            }
            model.addAttribute("transactionWithCardList", transactionWithCards);
            return "transactionList";
        }else{
            // No transactions found
            model.addAttribute("transactionWithCardList", new ArrayList<>());
            return "transactionList";
        }


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
