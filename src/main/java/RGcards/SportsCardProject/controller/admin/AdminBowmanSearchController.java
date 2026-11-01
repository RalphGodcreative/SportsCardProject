package RGcards.SportsCardProject.controller.admin;

import RGcards.SportsCardProject.service.BowmanSearchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/bowman-searches")
@RequiredArgsConstructor
public class AdminBowmanSearchController {

    static final List<Integer> PERIODS = List.of(7, 30, 90);
    private static final int TOP_LIMIT = 20;
    private static final int LATEST_LIMIT = 100;

    private final BowmanSearchHistoryService historyService;

    /** days only picks the period of the top lists; the latest list is always the last 100. */
    @GetMapping
    public String list(@RequestParam(defaultValue = "30") int days, Model model) {
        if (!PERIODS.contains(days)) {
            days = 30;
        }
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        model.addAttribute("topPlayers", historyService.findTopPlayers(since, TOP_LIMIT));
        model.addAttribute("topSets", historyService.findTopSets(since, TOP_LIMIT));
        model.addAttribute("latest", historyService.findLatest(LATEST_LIMIT));
        model.addAttribute("periods", PERIODS);
        model.addAttribute("days", days);
        return "admin/bowman-searches";
    }
}
