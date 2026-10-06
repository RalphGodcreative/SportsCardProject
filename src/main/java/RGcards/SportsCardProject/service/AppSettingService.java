package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dao.AppSettingRepository;
import RGcards.SportsCardProject.dao.UserRepository;
import RGcards.SportsCardProject.entity.AppSetting;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppSettingService {

    public static final String REGISTRATION_ENABLED = "registration.enabled";
    public static final String REGISTRATION_MAX_USERS = "registration.max-users";

    private final AppSettingRepository appSettingRepository;
    private final UserRepository userRepository;

    @Value("${app.registration.enabled:false}")
    private boolean registrationEnabledDefault;

    @Value("${app.registration.max-users:0}")
    private int maxUsersDefault;

    private volatile boolean registrationEnabled;
    private volatile int maxUsers;

    @PostConstruct
    void load() {
        registrationEnabled = appSettingRepository.findById(REGISTRATION_ENABLED)
                .map(s -> Boolean.parseBoolean(s.getValue()))
                .orElse(registrationEnabledDefault);
        maxUsers = appSettingRepository.findById(REGISTRATION_MAX_USERS)
                .map(s -> Integer.parseInt(s.getValue()))
                .orElse(maxUsersDefault);
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
