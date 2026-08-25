package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dao.AppSettingRepository;
import RGcards.SportsCardProject.dao.UserRepository;
import RGcards.SportsCardProject.entity.AppSetting;
import RGcards.SportsCardProject.entity.User;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UsageLimits {

    public static final String TEST_MAX_CARDS = "limits.test.max-cards";
    public static final String TEST_MAX_KEYWORDS = "limits.test.max-keywords";
    public static final String TEST_MAX_AI_CALLS = "limits.test.max-ai-calls-per-month";
    public static final String USER_MAX_CARDS = "limits.user.max-cards";
    public static final String USER_MAX_KEYWORDS = "limits.user.max-keywords";
    public static final String USER_MAX_AI_CALLS = "limits.user.max-ai-calls-per-month";

    private final AppSettingRepository appSettingRepository;
    private final UserRepository userRepository;

    @Value("${app.limits.test.max-cards}")
    private int testMaxCardsDefault;
    @Value("${app.limits.test.max-keywords}")
    private int testMaxKeywordsDefault;
    @Value("${app.limits.test.max-ai-calls-per-month}")
    private int testMaxAiCallsDefault;

    @Value("${app.limits.user.max-cards}")
    private int userMaxCardsDefault;
    @Value("${app.limits.user.max-keywords}")
    private int userMaxKeywordsDefault;
    @Value("${app.limits.user.max-ai-calls-per-month}")
    private int userMaxAiCallsDefault;

    private volatile int testMaxCards;
    private volatile int testMaxKeywords;
    private volatile int testMaxAiCalls;
    private volatile int userMaxCards;
    private volatile int userMaxKeywords;
    private volatile int userMaxAiCalls;

    @PostConstruct
    void load() {
        testMaxCards = loadInt(TEST_MAX_CARDS, testMaxCardsDefault);
        testMaxKeywords = loadInt(TEST_MAX_KEYWORDS, testMaxKeywordsDefault);
        testMaxAiCalls = loadInt(TEST_MAX_AI_CALLS, testMaxAiCallsDefault);
        userMaxCards = loadInt(USER_MAX_CARDS, userMaxCardsDefault);
        userMaxKeywords = loadInt(USER_MAX_KEYWORDS, userMaxKeywordsDefault);
        userMaxAiCalls = loadInt(USER_MAX_AI_CALLS, userMaxAiCallsDefault);
        log.info("Usage limits loaded: test=[{} cards, {} keywords, {} ai-calls], user=[{} cards, {} keywords, {} ai-calls]",
                testMaxCards, testMaxKeywords, testMaxAiCalls, userMaxCards, userMaxKeywords, userMaxAiCalls);
    }

    public int getTestMaxCards()    { return testMaxCards; }
    public int getTestMaxKeywords() { return testMaxKeywords; }
    public int getTestMaxAiCalls()  { return testMaxAiCalls; }
    public int getUserMaxCards()    { return userMaxCards; }
    public int getUserMaxKeywords() { return userMaxKeywords; }
    public int getUserMaxAiCalls()  { return userMaxAiCalls; }

    public void setTestMaxCards(int value)    { testMaxCards = value;    saveInt(TEST_MAX_CARDS, value); }
    public void setTestMaxKeywords(int value) { testMaxKeywords = value; saveInt(TEST_MAX_KEYWORDS, value); }
    public void setTestMaxAiCalls(int value)  { testMaxAiCalls = value;  saveInt(TEST_MAX_AI_CALLS, value); }
    public void setUserMaxCards(int value)    { userMaxCards = value;    saveInt(USER_MAX_CARDS, value); }
    public void setUserMaxKeywords(int value) { userMaxKeywords = value; saveInt(USER_MAX_KEYWORDS, value); }
    public void setUserMaxAiCalls(int value)  { userMaxAiCalls = value;  saveInt(USER_MAX_AI_CALLS, value); }

    public int maxCards(User user)    { return forRole(user, testMaxCards, userMaxCards); }
    public int maxKeywords(User user) { return forRole(user, testMaxKeywords, userMaxKeywords); }
    public int maxAiCalls(User user)  { return forRole(user, testMaxAiCalls, userMaxAiCalls); }

    private int forRole(User user, int testValue, int userValue) {
        return switch (user.getRole()) {
            case "ROLE_ADMIN" -> Integer.MAX_VALUE;
            case "ROLE_TEST" -> testValue;
            default -> userValue;
        };
    }

    private int loadInt(String key, int fallback) {
        return appSettingRepository.findById(key)
                .map(s -> Integer.parseInt(s.getValue()))
                .orElse(fallback);
    }

    private void saveInt(String key, int value) {
        AppSetting setting = appSettingRepository.findById(key)
                .orElseGet(() -> {
                    AppSetting s = new AppSetting();
                    s.setSettingKey(key);
                    return s;
                });
        setting.setValue(Integer.toString(value));
        appSettingRepository.save(setting);
        log.info("Admin set {} to {}", key, value);
    }

    @Transactional
    public void resetAllAiCallCounts() {
        userRepository.resetAllAiCallCounts();
    }
}
