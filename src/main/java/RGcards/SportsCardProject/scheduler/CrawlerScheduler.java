package RGcards.SportsCardProject.scheduler;

import RGcards.SportsCardProject.service.CrawlerService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CrawlerScheduler {

    private final CrawlerService crawlerService;

    @Scheduled(cron = "0 0 21 * * *")
    public void scheduledCrawl() throws MessagingException {
        log.info("Scheduled crawler started");
        crawlerService.runCrawlerForAllUsers();
    }
}
