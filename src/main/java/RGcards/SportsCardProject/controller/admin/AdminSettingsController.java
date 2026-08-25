package RGcards.SportsCardProject.controller.admin;

import RGcards.SportsCardProject.service.AppSettingService;
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

    @GetMapping
    public String view(Model model) {
        model.addAttribute("registrationEnabled", appSettingService.isRegistrationEnabled());
        return "adminSettings";
    }

    @PostMapping("/registration")
    public String setRegistration(@RequestParam boolean enabled) {
        appSettingService.setRegistrationEnabled(enabled);
        return "redirect:/admin/settings";
    }
}
