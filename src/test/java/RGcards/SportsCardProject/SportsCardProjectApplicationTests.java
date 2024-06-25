package RGcards.SportsCardProject;

import RGcards.SportsCardProject.component.CardComponent;
import RGcards.SportsCardProject.eto.Card;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class SportsCardProjectApplicationTests {

	@Autowired
	private CardComponent cardComponent;

	@Test
	void contextLoads() {
		System.out.println(cardComponent.findTransactionInfoByTransactionId(20));
	}



}
