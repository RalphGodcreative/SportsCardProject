package RGcards.SportsCardProject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@ComponentScan
@EnableScheduling
public class SportsCardProjectApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Taipei"));
		SpringApplication.run(SportsCardProjectApplication.class, args);
	}

}
