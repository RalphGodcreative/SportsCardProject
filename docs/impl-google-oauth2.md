# Implementation: Google OAuth2 Login

## 1. `pom.xml` — Add Dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

---

## 2. `application.properties` — Add OAuth2 Config

```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope=email,profile
```

Add `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` to `/etc/sportscard/env` on the GCP VM.

---

## 3. `User.java` — Allow Nullable Password for OAuth Users

Google users have no password. Change the `password` field to allow null and add a `provider` field to distinguish login types.

```java
@Column(nullable = true)
private String password;

@Column(nullable = false)
private String provider; // "local" or "google"
```

Write a Flyway migration to backfill existing rows:
```sql
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;
ALTER TABLE users ADD COLUMN provider VARCHAR(20) NOT NULL DEFAULT 'local';
```

---

## 4. New `CustomOAuth2UserService.java`

This class hooks into the OAuth2 login flow. On first Google login it auto-creates a local `User` record. On subsequent logins it just loads the existing record.

```java
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name  = oAuth2User.getAttribute("name");

        userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(name);
            newUser.setPassword(null);
            newUser.setProvider("google");
            newUser.setRole("ROLE_USER");
            return userRepository.save(newUser);
        });

        return oAuth2User;
    }
}
```

---

## 5. `SecurityConfig.java` — Add OAuth2 Login

Inject `CustomOAuth2UserService` and chain `.oauth2Login()` into the existing `filterChain`.

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AppUserDetailsService userDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService; // add this

    // ... passwordEncoder and authenticationProvider beans unchanged ...

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/login", "/register",
                    "/assets/**", "/css/**", "/js/**", "/images/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2      // add this block
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .defaultSuccessUrl("/", true)
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );
        return http.build();
    }
}
```

---

## 6. Login Page — Add Google Button

Add this button to `login.html`. Spring Security handles the redirect automatically via the registered `/oauth2/authorization/google` endpoint.

```html
<a href="/oauth2/authorization/google" class="btn btn-google">
    Login with Google
</a>
```

---

## Notes

- Google users will have `password = null` in the DB — this is expected. `DaoAuthenticationProvider` only runs for form login, so it will never try to validate their null password.
- The authentication principal for Google logins is a `DefaultOAuth2User`, not your `User` entity. Any controller code that casts the principal to `User` will need to handle both types.
