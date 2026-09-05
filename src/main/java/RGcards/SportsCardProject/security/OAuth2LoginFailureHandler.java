package RGcards.SportsCardProject.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Reports why a Google round trip failed. A failed link goes back to the profile page,
 * a failed sign-in back to the login page, both carrying the error code so the template
 * can explain itself rather than falling back on "invalid email or password".
 */
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        boolean linking = AccountLinkSession.pendingUserId(request) != null;
        AccountLinkSession.clear(request);

        String code = exception instanceof OAuth2AuthenticationException oauthException
                ? oauthException.getError().getErrorCode()
                : "failed";
        String encoded = URLEncoder.encode(code, StandardCharsets.UTF_8);

        redirectStrategy.sendRedirect(request, response, linking
                ? "/user/edit?linkError=" + encoded
                : "/login?oauthError=" + encoded);
    }
}
