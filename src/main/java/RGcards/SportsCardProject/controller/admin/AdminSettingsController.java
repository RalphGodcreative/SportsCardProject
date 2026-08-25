package RGcards.SportsCardProject.controller.admin;

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
        model.addAttribute("testMaxCards", usageLimits.getTestMaxCards());
        model.addAttribute("testMaxKeywords", usageLimits.getTestMaxKeywords());
        model.addAttribute("testMaxAiCalls", usageLimits.getTestMaxAiCalls());
        model.addAttribute("userMaxCards", usageLimits.getUserMaxCards());
        model.addAttribute("userMaxKeywords", usageLimits.getUserMaxKeywords());
        model.addAttribute("userMaxAiCalls", usageLimits.getUserMaxAiCalls());
        return "adminSettings";
    }

    @PostMapping("/registration")
    public String setRegistration(@RequestParam boolean enabled) {
        appSettingService.setRegistrationEnabled(enabled);
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
