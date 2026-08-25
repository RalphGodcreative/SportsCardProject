package RGcards.SportsCardProject.controller.admin;

import RGcards.SportsCardProject.service.UsageLimits;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/admin/usage-limits")
@RequiredArgsConstructor
@Slf4j
public class AdminUsageLimitsController {

    private final UsageLimits usageLimits;

    @PostMapping("/reset-ai-calls")
    @ResponseBody
    public Boolean resetAiCalls() {
        try {
            log.info("Admin manually triggered AI call count reset");
            usageLimits.resetAllAiCallCounts();
            return true;
        } catch (Exception e) {
            log.error("Manual AI call count reset failed", e);
            return false;
        }
    }
}
