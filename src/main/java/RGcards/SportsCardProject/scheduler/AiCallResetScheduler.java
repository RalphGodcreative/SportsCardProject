package RGcards.SportsCardProject.scheduler;

import RGcards.SportsCardProject.service.UsageLimits;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiCallResetScheduler {

    private final UsageLimits usageLimits;

    @Scheduled(cron = "0 0 0 1 * *")
    public void scheduledReset() {
        log.info("Scheduled AI call count reset started");
        usageLimits.resetAllAiCallCounts();
        log.info("Scheduled AI call count reset finished");
    }
}
