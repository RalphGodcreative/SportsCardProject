package RGcards.SportsCardProject.config;

import org.springframework.beans.factory.annotation.Value;
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
}
