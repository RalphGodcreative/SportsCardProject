package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.entity.User;
import RGcards.SportsCardProject.service.CardService;
import RGcards.SportsCardProject.service.TagService;
import RGcards.SportsCardProject.entity.Card;
import RGcards.SportsCardProject.entity.SaleWithCard;
import RGcards.SportsCardProject.entity.Transaction;
import RGcards.SportsCardProject.dto.TransactionWithCard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @Autowired
    private TagService tagService;

    @GetMapping("")
    public String main(Model model, @AuthenticationPrincipal User currentUser) {
        model.addAttribute("lastCard", cardService.getLastCard(currentUser.getId()));
        model.addAttribute("tags", tagService.findAllForUser(currentUser.getId()));
        return "cardMain";
    }

    @GetMapping("/addNewCard")
    public String addNewCard(Model model, @AuthenticationPrincipal User currentUser) {
        model.addAttribute("tags", tagService.findAllForUser(currentUser.getId()));
        return "addCardPage";
    }

    @GetMapping("/saveCard")
    public String saveCard(@ModelAttribute("year") String year, @ModelAttribute("publisher") String publisher,
                           @ModelAttribute("set") String set, @ModelAttribute("player") String player,
                           @RequestParam(name = "auto", defaultValue = "false") Boolean auto, @ModelAttribute("insert") String insert,
                           @ModelAttribute("parallel") String parallel, @ModelAttribute("numbered") String numbered,
                           @ModelAttribute("sports") String sports, @ModelAttribute("grade") String grade,
                           @RequestParam(name = "value", defaultValue = "") Double value, @ModelAttribute("note") String note,
                           @RequestParam(name = "tagIds", required = false) List<Long> tagIds,
                           @AuthenticationPrincipal User currentUser
    ) {
        Card card = new Card(year, publisher, set, player, auto, insert, parallel, numbered, sports, grade, value, note);
        card.setUserId(currentUser.getId());
        card.setTags(tagService.resolveOwnedTags(tagIds, currentUser.getId()));
        cardService.saveCard(card);
        return "redirect:/card/cards";
    }

    @PostMapping("/updateCard")
    public String updateCard(@RequestParam(name = "id", defaultValue = "") String id, @ModelAttribute("year") String year, @ModelAttribute("publisher") String publisher,
                             @ModelAttribute("set") String set, @ModelAttribute("player") String player,
                             @RequestParam(name = "auto", defaultValue = "false") Boolean auto, @ModelAttribute("insert") String insert,
                             @ModelAttribute("parallel") String parallel, @ModelAttribute("numbered") String numbered,
                             @ModelAttribute("sports") String sports, @ModelAttribute("grade") String grade,
                             @RequestParam(name = "value", defaultValue = "") Double value, @ModelAttribute("note") String note,
                             @RequestParam(name = "tagIds", required = false) List<Long> tagIds,
                             @AuthenticationPrincipal User currentUser
    ) {
        Card card = new Card(Integer.parseInt(id), year, publisher, set, player, auto, insert, parallel, numbered, sports, grade, value, note);
        card.setUserId(currentUser.getId());
        card.setTags(tagService.resolveOwnedTags(tagIds, currentUser.getId()));
        cardService.saveCard(card);
        return "redirect:/card/cards";
    }

    @PostMapping("/saveTransaction")
    public String saveTransaction(@RequestBody TransactionWithCard transactionWithCard,
                                  @AuthenticationPrincipal User currentUser) {
        // Posted tag ids are untrusted — resolve them to owned tags before saving.
        for (Card card : transactionWithCard.getCards()) {
            card.setTags(tagService.resolveOwnedTags(card.getTagIds(), currentUser.getId()));
        }
        cardService.saveTransactionWithCard(transactionWithCard, currentUser.getId());
        return "redirect:/card/cards";
    }

    @PostMapping("/saveSale")
    public String saveSale(@RequestBody SaleWithCard saleWithCard,
                           @AuthenticationPrincipal User currentUser) {
        cardService.saveSaleWithCard(saleWithCard, currentUser.getId());
        return "redirect:/card/cards";
    }

    @GetMapping("/allCard")
    public String allCards(Model model, @AuthenticationPrincipal User currentUser) {
        List<Card> cards = cardService.getAllCardsSortById(currentUser.getId());
        int cardCounts = cardService.findCardsCount(currentUser.getId());
        model.addAttribute("cards", cards);
        model.addAttribute("cardCounts", cardCounts);
        model.addAttribute("tags", tagService.findAllForUser(currentUser.getId()));
        return "allCard";
    }

    @GetMapping("/cards")
    public String allCardsByPage(Model model, @RequestParam(defaultValue = "1") int page,
                                 @AuthenticationPrincipal User currentUser) {
        List<Card> cards = cardService.findCardsByPage(page, currentUser.getId());
        int cardCounts = cardService.findCardsCount(currentUser.getId());
        model.addAttribute("cards", cards);
        model.addAttribute("cardCounts", cardCounts);
        model.addAttribute("page", page);
        model.addAttribute("tags", tagService.findAllForUser(currentUser.getId()));
        return "pagingAllCard";
    }

    @GetMapping("/cardTransaction/{cardId}")
    public String getTransactionOfCard(Model model, @PathVariable String cardId,
                                        @AuthenticationPrincipal User currentUser) {
        List<Transaction> transactions = cardService.getTransactionByCardId(Integer.parseInt(cardId));
        List<TransactionWithCard> transactionWithCards = new ArrayList<>();
        for (Transaction transaction : transactions) {
            List<Card> cards = cardService.findCardsByTransactionId(transaction.getId());
            transactionWithCards.add(new TransactionWithCard(transaction, cards));
        }
        model.addAttribute("transactionWithCardList", transactionWithCards);
        model.addAttribute("tags", tagService.findAllForUser(currentUser.getId()));
        return "transactionDetail";
    }

    @GetMapping("/addTransaction")
    public String addTransaction(Model model, @AuthenticationPrincipal User currentUser) {
        model.addAttribute("tags", tagService.findAllForUser(currentUser.getId()));
        return "addTransactionPage";
    }

    @GetMapping("/searchCard")
    public String searchCard(Model model, @AuthenticationPrincipal User currentUser) {
        model.addAttribute("tags", tagService.findAllForUser(currentUser.getId()));
        return "searchCardPage";
    }

    @GetMapping("/sellTransaction")
    public String sellTransaction(Model model, @AuthenticationPrincipal User currentUser) {
        model.addAttribute("tags", tagService.findAllForUser(currentUser.getId()));
        return "sellTransactionPage";
    }
}
