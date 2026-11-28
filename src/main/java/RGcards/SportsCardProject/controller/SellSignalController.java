package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.entity.User;
import RGcards.SportsCardProject.service.AppSettingService;
import RGcards.SportsCardProject.service.SellSignalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.format.DateTimeFormatter;

/** Admin-only in v1: "/sell-signals/**" is in the ROLE_ADMIN matcher in SecurityConfig. */
@Slf4j
@Controller
@RequestMapping("/sell-signals")
@RequiredArgsConstructor
public class SellSignalController {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SellSignalService sellSignalService;
    private final AppSettingService appSettingService;

    @GetMapping
    public String page(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("page", sellSignalService.page(user));
        model.addAttribute("currentFx", appSettingService.getUsdToTwd());
        return "sellSignals";
    }

    @PostMapping("/run")
    public String run(@AuthenticationPrincipal User user, RedirectAttributes redirectAttributes) {
        try {
            SellSignalService.RunOutcome outcome = sellSignalService.run(user);
            if (!outcome.started()) {
                var next = outcome.run() == null ? null
                        : SellSignalService.nextRunAt(user, outcome.run(), java.time.LocalDateTime.now());
                redirectAttributes.addFlashAttribute("info", next != null
                        ? "下一次掃描時間：" + next.format(TIME)
                        : "掃描進行中，請稍候再重新整理。");
            }
        } catch (RuntimeException e) {
            log.error("Sell signals run failed for user {}", user.getId(), e);
            redirectAttributes.addFlashAttribute("error", "掃描失敗，請稍後再試。");
        }
        return "redirect:/sell-signals";
    }
}
