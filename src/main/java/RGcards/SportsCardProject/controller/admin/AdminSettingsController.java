package RGcards.SportsCardProject.controller.admin;

import RGcards.SportsCardProject.enums.GeminiModel;
import RGcards.SportsCardProject.service.AppSettingService;
import RGcards.SportsCardProject.service.UsageLimits;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
@Slf4j
public class AdminSettingsController {

    private final AppSettingService appSettingService;
    private final UsageLimits usageLimits;

    @GetMapping
    public String view(Model model) {
        model.addAttribute("registrationEnabled", appSettingService.isRegistrationEnabled());
        model.addAttribute("maxUsers", appSettingService.getMaxUsers());
        model.addAttribute("userCount", appSettingService.getUserCount());
        model.addAttribute("testMaxCards", usageLimits.getTestMaxCards());
        model.addAttribute("testMaxKeywords", usageLimits.getTestMaxKeywords());
        model.addAttribute("testMaxAiCalls", usageLimits.getTestMaxAiCalls());
        model.addAttribute("userMaxCards", usageLimits.getUserMaxCards());
        model.addAttribute("userMaxKeywords", usageLimits.getUserMaxKeywords());
        model.addAttribute("userMaxAiCalls", usageLimits.getUserMaxAiCalls());
        model.addAttribute("usdToTwd", appSettingService.getUsdToTwd());
        model.addAttribute("geminiModels", GeminiModel.values());
        model.addAttribute("geminiModel", appSettingService.getGeminiModel());
        return "adminSettings";
    }

    @PostMapping("/gemini-model")
    public String setGeminiModel(@RequestParam String model) {
        GeminiModel.fromId(model).ifPresentOrElse(
                appSettingService::setGeminiModel,
                () -> log.warn("Ignored unknown Gemini model: {}", model));
        return "redirect:/admin/settings";
    }

    @PostMapping("/registration")
    public String setRegistration(@RequestParam boolean enabled) {
        appSettingService.setRegistrationEnabled(enabled);
        return "redirect:/admin/settings";
    }

    @PostMapping("/max-users")
    public String setMaxUsers(@RequestParam int maxUsers) {
        appSettingService.setMaxUsers(Math.max(0, maxUsers));
        return "redirect:/admin/settings";
    }

    @PostMapping("/sell-signals")
    public String setSellSignals(@RequestParam BigDecimal usdToTwd) {
        if (usdToTwd.signum() > 0) {
            appSettingService.setUsdToTwd(usdToTwd);
        }
        return "redirect:/admin/settings";
    }

    @PostMapping("/usage-limits")
    public String setUsageLimits(
            @RequestParam int testMaxCards, @RequestParam int testMaxKeywords, @RequestParam int testMaxAiCalls,
            @RequestParam int userMaxCards, @RequestParam int userMaxKeywords, @RequestParam int userMaxAiCalls
    ) {
        usageLimits.setTestMaxCards(testMaxCards);
        usageLimits.setTestMaxKeywords(testMaxKeywords);
        usageLimits.setTestMaxAiCalls(testMaxAiCalls);
        usageLimits.setUserMaxCards(userMaxCards);
        usageLimits.setUserMaxKeywords(userMaxKeywords);
        usageLimits.setUserMaxAiCalls(userMaxAiCalls);
        return "redirect:/admin/settings";
    }
}
