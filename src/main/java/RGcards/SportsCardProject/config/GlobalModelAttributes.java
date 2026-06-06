package RGcards.SportsCardProject.config;

import RGcards.SportsCardProject.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    @Value("${app.registration.enabled:false}")
    private boolean registrationEnabled;

    @ModelAttribute("registrationEnabled")
    public boolean registrationEnabled() {
        return registrationEnabled;
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
