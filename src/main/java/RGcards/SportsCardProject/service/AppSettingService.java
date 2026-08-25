package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dao.AppSettingRepository;
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

    private final AppSettingRepository appSettingRepository;

    @Value("${app.registration.enabled:false}")
    private boolean registrationEnabledDefault;

    private volatile boolean registrationEnabled;

    @PostConstruct
    void load() {
        registrationEnabled = appSettingRepository.findById(REGISTRATION_ENABLED)
                .map(s -> Boolean.parseBoolean(s.getValue()))
                .orElse(registrationEnabledDefault);
        log.info("Registration enabled: {}", registrationEnabled);
    }

    public boolean isRegistrationEnabled() {
        return registrationEnabled;
    }

    public void setRegistrationEnabled(boolean enabled) {
        AppSetting setting = appSettingRepository.findById(REGISTRATION_ENABLED)
                .orElseGet(() -> {
                    AppSetting s = new AppSetting();
                    s.setSettingKey(REGISTRATION_ENABLED);
                    return s;
                });
        setting.setValue(Boolean.toString(enabled));
        appSettingRepository.save(setting);
        registrationEnabled = enabled;
        log.info("Admin set registration enabled: {}", enabled);
    }
}
