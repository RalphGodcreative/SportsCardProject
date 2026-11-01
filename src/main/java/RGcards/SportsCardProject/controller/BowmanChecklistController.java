package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.entity.BowmanChecklist;
import RGcards.SportsCardProject.entity.User;
import RGcards.SportsCardProject.service.BowmanChecklistSearchService;
import RGcards.SportsCardProject.service.BowmanSearchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final BowmanSearchHistoryService searchHistoryService;

    /**
     * Two separate searches share this page: by player (player param) or by set
     * (year and/or setName params). With neither, nothing is searched and no
     * results are shown. Every search that runs is recorded in the search history;
     * user is null for guests, since /bowman is public.
     */
    @GetMapping
    public String search(@AuthenticationPrincipal User user,
                         @RequestParam(required = false) String player,
                         @RequestParam(required = false) Short year,
                         @RequestParam(required = false) String setName,
                         Model model) {
        boolean byPlayer = player != null && !player.isBlank();
        boolean bySet = year != null || (setName != null && !setName.isBlank());
        Long userId = user == null ? null : user.getId();

        List<BowmanChecklist> results = null;
        if (byPlayer) {
            results = searchService.searchByPlayer(player);
            searchHistoryService.recordPlayerSearch(userId, player);
        } else if (bySet) {
            results = searchService.searchBySet(year, setName);
            searchHistoryService.recordSetSearch(userId, year, setName);
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
