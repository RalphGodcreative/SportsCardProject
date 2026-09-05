package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dao.UserRepository;
import RGcards.SportsCardProject.entity.User;
import RGcards.SportsCardProject.security.AccountLinkSession;
import RGcards.SportsCardProject.security.OAuth2UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final AppSettingService appSettingService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String sub   = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name  = oAuth2User.getAttribute("name");
        boolean emailVerified = Boolean.TRUE.equals(oAuth2User.getAttribute("email_verified"));

        if (sub == null || sub.isBlank()) {
            throw error("invalid_user_info", "Google did not return an account id");
        }

        // A signed-in user who asked to connect Google is linking, not signing in: the
        // Google email may be a completely different address from their account email.
        Long linkingUserId = AccountLinkSession.pendingUserId();
        User user = linkingUserId != null
                ? connect(linkingUserId, sub, email, emailVerified)
                : signIn(sub, email, name, emailVerified);

        return new OAuth2UserPrincipal(user, oAuth2User.getAttributes());
    }

    private User signIn(String sub, String email, String name, boolean emailVerified) {
        // Match on the subject id first: it is stable even if the user later changes
        // the email on either side, so a linked account stays linked.
        User user = userRepository.findByGoogleSub(sub).orElse(null);
        boolean needsLink = false;

        if (user == null) {
            requireEmail(email);
            // Only a verified email may claim an existing account or open a new one.
            // Without this an unverified address could be used to take over a local account.
            requireVerifiedEmail(emailVerified);

            User existing = userRepository.findByEmailIgnoreCase(email.toLowerCase()).orElse(null);
            if (existing != null) {
                user = existing;
                needsLink = true;
            } else {
                user = register(email.toLowerCase(), name, sub);
            }
        }

        requireEnabled(user);

        // Attach Google to the existing account, leaving its password login intact.
        // Done after the enabled check so a blocked account is never modified.
        if (needsLink) {
            user.setGoogleSub(sub);
            user = userRepository.save(user);
        }
        return user;
    }

    private User connect(Long userId, String sub, String email, boolean emailVerified) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> error("link_failed", "Your session is no longer valid"));

        requireEnabled(user);
        requireEmail(email);
        requireVerifiedEmail(emailVerified);

        if (sub.equals(user.getGoogleSub())) {
            return user; // already connected to this very Google account
        }
        if (user.getGoogleSub() != null) {
            throw error("already_linked", "This account is already connected to a different Google account");
        }
        userRepository.findByGoogleSub(sub).ifPresent(owner -> {
            throw error("google_account_taken", "That Google account is already connected to another account");
        });
        // Refuse an email another account already signs in with, otherwise that account
        // would become unreachable by Google: the subject id match here would win.
        userRepository.findByEmailIgnoreCase(email.toLowerCase()).ifPresent(owner -> {
            if (!owner.getId().equals(userId)) {
                throw error("email_taken", "That Google email address already belongs to another account");
            }
        });

        user.setGoogleSub(sub);
        return userRepository.save(user);
    }

    private User register(String email, String name, String sub) {
        if (!appSettingService.isRegistrationEnabled()) {
            throw error("registration_disabled", "Registration is currently closed");
        }
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setUsername(resolveUsername(name, email));
        newUser.setProvider("google");
        newUser.setRole("ROLE_USER");
        newUser.setGoogleSub(sub);
        return userRepository.save(newUser);
    }

    private void requireEmail(String email) {
        if (email == null || email.isBlank()) {
            throw error("invalid_user_info", "Google did not return an email address");
        }
    }

    private void requireVerifiedEmail(boolean emailVerified) {
        if (!emailVerified) {
            throw error("email_unverified", "Your Google email address is not verified");
        }
    }

    private void requireEnabled(User user) {
        if (!user.isEnabled()) {
            throw error("account_disabled", "This account has been disabled");
        }
    }

    private OAuth2AuthenticationException error(String code, String message) {
        return new OAuth2AuthenticationException(new OAuth2Error(code), message);
    }

    private String resolveUsername(String name, String email) {
        if (name != null && !userRepository.existsByUsernameAndIdNot(name, -1L)) {
            return name;
        }
        String base = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        String candidate = base;
        int i = 2;
        while (userRepository.existsByUsernameAndIdNot(candidate, -1L)) {
            candidate = base + i++;
        }
        return candidate;
    }
}
