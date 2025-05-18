package RGcards.SportsCardProject;

import RGcards.SportsCardProject.bot.YahooAuctionBot;
import RGcards.SportsCardProject.entity.SearchKeyword;
import RGcards.SportsCardProject.service.CardService;
import RGcards.SportsCardProject.entity.SearchProduct;
import RGcards.SportsCardProject.email.EmailService;
import RGcards.SportsCardProject.service.CrawlerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class SportsCardProjectApplicationTests {

	@Autowired
	private CrawlerService crawlerService;

	@Autowired
	private EmailService emailService;

	@Autowired
	private YahooAuctionBot yahooAuctionBot;

	@Test
	void contextLoads() {
		SearchKeyword keyword = crawlerService.getSearchKeywordByKeyword("岱縈");
		System.out.println(crawlerService.searchResultForKeyword(keyword));

	}



}
