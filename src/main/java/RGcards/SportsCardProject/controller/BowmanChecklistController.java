package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.entity.BowmanChecklist;
import RGcards.SportsCardProject.service.BowmanChecklistSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/bowman")
@RequiredArgsConstructor
public class BowmanChecklistController {

    private final BowmanChecklistSearchService searchService;

    /**
     * Two separate searches share this page: by player (player param) or by set
     * (year and/or setName params). With neither, nothing is searched and no
     * results are shown.
     */
    @GetMapping
    public String search(@RequestParam(required = false) String player,
                         @RequestParam(required = false) Short year,
                         @RequestParam(required = false) String setName,
                         Model model) {
        boolean byPlayer = player != null && !player.isBlank();
        boolean bySet = year != null || (setName != null && !setName.isBlank());

        List<BowmanChecklist> results = null;
        if (byPlayer) {
            results = searchService.searchByPlayer(player);
        } else if (bySet) {
            results = searchService.searchBySet(year, setName);
        }

        model.addAttribute("results", results);
        model.addAttribute("player", player);
        model.addAttribute("year", year);
        model.addAttribute("setName", setName);
        model.addAttribute("years", searchService.findYears());
        model.addAttribute("setNames", searchService.findSetNames());
        return "bowman/search";
    }
}
