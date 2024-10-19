package RGcards.SportsCardProject;

import RGcards.SportsCardProject.component.CardComponent;
import RGcards.SportsCardProject.eto.Card;
import RGcards.SportsCardProject.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class SportsCardProjectApplicationTests {

	@Autowired
	private CardComponent cardComponent;

	@Autowired
	private EmailService emailService;

	@Test
	void contextLoads() {

		String mailUsername = System.getenv("MAIL_USERNAME");
		String mailPassword = System.getenv("MAIL_PASSWORD");

		System.out.println("MAIL_USERNAME: " + mailUsername);
		System.out.println("MAIL_PASSWORD: " + mailPassword);

		emailService.sendSimpleEmail("ralph8002@gmail.com" , "testing" , "what's up \n it's me again");


	}



}
