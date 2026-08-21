package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.entity.Card;
import RGcards.SportsCardProject.entity.Tag;
import RGcards.SportsCardProject.entity.User;
import RGcards.SportsCardProject.service.CardService;
import RGcards.SportsCardProject.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/tag")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;
    private final CardService cardService;

    @GetMapping
    public String tagHome(Model model, @AuthenticationPrincipal User currentUser) {
        List<Tag> tags = tagService.findAllForUser(currentUser.getId());
        Map<Long, Long> cardCounts = new LinkedHashMap<>();
        long totalTagged = 0;
        for (Tag tag : tags) {
            long count = cardService.countCardsWithTag(tag.getId(), currentUser.getId());
            cardCounts.put(tag.getId(), count);
            totalTagged += count;
        }
        model.addAttribute("tags", tags);
        model.addAttribute("cardCounts", cardCounts);
        model.addAttribute("totalTagged", totalTagged);
        return "tag/list";
    }

    @GetMapping("/{id}/cards")
    public String tagCards(@PathVariable long id, Model model, @AuthenticationPrincipal User currentUser) {
        Tag tag = tagService.findByIdAndUserId(id, currentUser.getId()).orElse(null);
        if (tag == null) {
            return "redirect:/tag";
        }
        List<Card> cards = cardService.getCardsByTag(id, currentUser.getId());
        model.addAttribute("tag", tag);
        model.addAttribute("cards", cards);
        model.addAttribute("tags", tagService.findAllForUser(currentUser.getId()));
        return "tag/cards";
    }

    @PutMapping("/add")
    @ResponseBody
    public Boolean addTag(@RequestParam(name = "name") String name,
                          @AuthenticationPrincipal User currentUser) {
        return tagService.create(name, currentUser.getId()) != null;
    }

    @PutMapping("/rename")
    @ResponseBody
    public Boolean renameTag(@RequestParam(name = "tagId") long tagId,
                             @RequestParam(name = "name") String name,
                             @AuthenticationPrincipal User currentUser) {
        return tagService.rename(tagId, name, currentUser.getId()).isPresent();
    }

    @DeleteMapping("/delete")
    @ResponseBody
    public Boolean deleteTag(@RequestParam(name = "tagId") long tagId,
                             @AuthenticationPrincipal User currentUser) {
        tagService.delete(tagId, currentUser.getId());
        return true;
    }
}
