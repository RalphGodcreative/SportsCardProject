package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dao.UserRepository;
import RGcards.SportsCardProject.entity.User;
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

        String email = oAuth2User.<String>getAttribute("email").toLowerCase();
        String name  = oAuth2User.getAttribute("name");

        User user = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            if (!appSettingService.isRegistrationEnabled()) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("registration_disabled"), "Registration is currently closed");
            }
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(resolveUsername(name, email));
            newUser.setProvider("google");
            newUser.setRole("ROLE_USER");
            return userRepository.save(newUser);
        });

        if (!user.isEnabled()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("account_disabled"), "This account has been disabled");
        }

        return new OAuth2UserPrincipal(user, oAuth2User.getAttributes());
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
