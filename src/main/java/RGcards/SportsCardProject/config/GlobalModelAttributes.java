package RGcards.SportsCardProject.config;

import RGcards.SportsCardProject.entity.User;
import RGcards.SportsCardProject.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final AppSettingService appSettingService;

    @ModelAttribute("registrationEnabled")
    public boolean registrationEnabled() {
        return appSettingService.isRegistrationEnabled();
    }

    @ModelAttribute("displayUsername")
    public String displayUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user.getDisplayName();
        }
        if (principal instanceof OAuth2User oauth2User) {
            return oauth2User.getAttribute("name");
        }
        return null;
    }
}
