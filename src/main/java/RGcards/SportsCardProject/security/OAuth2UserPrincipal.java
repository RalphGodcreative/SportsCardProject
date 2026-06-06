package RGcards.SportsCardProject.security;

import RGcards.SportsCardProject.entity.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

public class OAuth2UserPrincipal extends User implements OAuth2User {

    private final Map<String, Object> attributes;

    public OAuth2UserPrincipal(User user, Map<String, Object> attributes) {
        setId(user.getId());
        setEmail(user.getEmail());
        setUsername(user.getDisplayName());
        setPassword(user.getPassword());
        setRole(user.getRole());
        setProvider(user.getProvider());
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        String name = getAttribute("name");
        return name != null ? name : getDisplayName();
    }
}
