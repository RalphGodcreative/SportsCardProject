package RGcards.SportsCardProject.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Sends a plain Google sign-in home, and a "connect Google" round trip back to the profile. */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        boolean linking = AccountLinkSession.pendingUserId(request) != null;
        AccountLinkSession.clear(request);
        redirectStrategy.sendRedirect(request, response, linking ? "/user/edit?linked" : "/");
    }
}
