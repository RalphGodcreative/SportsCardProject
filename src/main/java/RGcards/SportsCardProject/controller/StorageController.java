package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.entity.Card;
import RGcards.SportsCardProject.entity.Storage;
import RGcards.SportsCardProject.entity.User;
import RGcards.SportsCardProject.service.CardService;
import RGcards.SportsCardProject.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;
    private final CardService cardService;

    @GetMapping
    public String storageHome(Model model, @AuthenticationPrincipal User currentUser) {
        List<Storage> storages = storageService.findAllForUser(currentUser.getId());
        Map<Long, Long> cardCounts = new LinkedHashMap<>();
        long totalCards = 0;
        for (Storage storage : storages) {
            long count = cardService.countCardsInStorage(storage.getId(), currentUser.getId());
            cardCounts.put(storage.getId(), count);
            totalCards += count;
        }
        model.addAttribute("storages", storages);
        model.addAttribute("cardCounts", cardCounts);
        model.addAttribute("totalCards", totalCards);
        return "storage/list";
    }

    @GetMapping("/{id}/cards")
    public String storageCards(@PathVariable long id, Model model, @AuthenticationPrincipal User currentUser) {
        Storage storage = storageService.findByIdAndUserId(id, currentUser.getId()).orElse(null);
        if (storage == null) {
            return "redirect:/storage";
        }
        List<Card> cards = cardService.getCardsByStorage(id, currentUser.getId());
        model.addAttribute("storage", storage);
        model.addAttribute("cards", cards);
        model.addAttribute("storageNames", storageService.findNameMapForUser(currentUser.getId()));
        return "storage/cards";
    }

    @PutMapping("/add")
    @ResponseBody
    public Boolean addStorage(@RequestParam(name = "name") String name,
                               @AuthenticationPrincipal User currentUser) {
        return storageService.create(name, currentUser.getId()) != null;
    }

    @PutMapping("/rename")
    @ResponseBody
    public Boolean renameStorage(@RequestParam(name = "storageId") long storageId,
                                  @RequestParam(name = "name") String name,
                                  @AuthenticationPrincipal User currentUser) {
        return storageService.rename(storageId, name, currentUser.getId()).isPresent();
    }

    @DeleteMapping("/delete")
    @ResponseBody
    public Boolean deleteStorage(@RequestParam(name = "storageId") long storageId,
                                  @AuthenticationPrincipal User currentUser) {
        storageService.delete(storageId, currentUser.getId());
        return true;
    }
}
