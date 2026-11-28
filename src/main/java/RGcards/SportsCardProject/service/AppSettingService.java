package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dao.AppSettingRepository;
import RGcards.SportsCardProject.dao.UserRepository;
import RGcards.SportsCardProject.entity.AppSetting;
import RGcards.SportsCardProject.enums.GeminiModel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppSettingService {

    public static final String REGISTRATION_ENABLED = "registration.enabled";
    public static final String REGISTRATION_MAX_USERS = "registration.max-users";
    public static final String SELL_SIGNALS_USD_TO_TWD = "sell_signals.usd_to_twd";
    public static final String GEMINI_MODEL = "gemini.model";

    private final AppSettingRepository appSettingRepository;
    private final UserRepository userRepository;

    @Value("${app.registration.enabled:false}")
    private boolean registrationEnabledDefault;

    @Value("${app.registration.max-users:0}")
    private int maxUsersDefault;

    @Value("${app.sell-signals.usd-to-twd:32.0}")
    private BigDecimal usdToTwdDefault;

    private volatile boolean registrationEnabled;
    private volatile int maxUsers;
    private volatile BigDecimal usdToTwd;
    private volatile GeminiModel geminiModel;

    @PostConstruct
    void load() {
        registrationEnabled = appSettingRepository.findById(REGISTRATION_ENABLED)
                .map(s -> Boolean.parseBoolean(s.getValue()))
                .orElse(registrationEnabledDefault);
        maxUsers = appSettingRepository.findById(REGISTRATION_MAX_USERS)
                .map(s -> Integer.parseInt(s.getValue()))
                .orElse(maxUsersDefault);
        usdToTwd = appSettingRepository.findById(SELL_SIGNALS_USD_TO_TWD)
                .map(s -> new BigDecimal(s.getValue()))
                .orElse(usdToTwdDefault);
        // A saved model that is no longer in the catalog falls back to the default.
        geminiModel = appSettingRepository.findById(GEMINI_MODEL)
                .flatMap(s -> GeminiModel.fromId(s.getValue()))
                .orElse(GeminiModel.DEFAULT);
        log.info("Gemini model: {}", geminiModel.getId());
        log.info("Registration enabled: {}, max users: {}", registrationEnabled, maxUsers == 0 ? "unlimited" : maxUsers);
    }

    public boolean isRegistrationEnabled() {
        return registrationEnabled;
    }

    public void setRegistrationEnabled(boolean enabled) {
        save(REGISTRATION_ENABLED, Boolean.toString(enabled));
        registrationEnabled = enabled;
        log.info("Admin set registration enabled: {}", enabled);
    }

    /** Total accounts allowed to exist; 0 means no cap. */
    public int getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(int value) {
        if (value < 0) throw new IllegalArgumentException("max users must be >= 0");
        save(REGISTRATION_MAX_USERS, Integer.toString(value));
        maxUsers = value;
        log.info("Admin set max users: {}", value == 0 ? "unlimited" : value);
    }

    public long getUserCount() {
        return userRepository.count();
    }

    /** True when a cap is set and the number of accounts has reached it. */
    public boolean isUserCapReached() {
        int cap = maxUsers;
        return cap > 0 && userRepository.count() >= cap;
    }

    /** Whether a new account may be created right now: switch on and cap not reached. */
    public boolean isRegistrationOpen() {
        return registrationEnabled && !isUserCapReached();
    }

    /** USD to TWD rate used by Sell Signals to compare eBay prices with TWD cost. */
    public BigDecimal getUsdToTwd() {
        return usdToTwd;
    }

    public void setUsdToTwd(BigDecimal value) {
        if (value == null || value.signum() <= 0) throw new IllegalArgumentException("rate must be > 0");
        save(SELL_SIGNALS_USD_TO_TWD, value.toPlainString());
        usdToTwd = value;
        log.info("Admin set USD to TWD rate: {}", value);
    }

    /** Gemini model used by every AI feature. */
    public GeminiModel getGeminiModel() {
        return geminiModel;
    }

    public void setGeminiModel(GeminiModel model) {
        if (model == null) throw new IllegalArgumentException("model is required");
        save(GEMINI_MODEL, model.getId());
        geminiModel = model;
        log.info("Admin set Gemini model: {}", model.getId());
    }

    private void save(String key, String value) {
        AppSetting setting = appSettingRepository.findById(key)
                .orElseGet(() -> {
                    AppSetting s = new AppSetting();
                    s.setSettingKey(key);
                    return s;
                });
        setting.setValue(value);
        appSettingRepository.save(setting);
    }
}
