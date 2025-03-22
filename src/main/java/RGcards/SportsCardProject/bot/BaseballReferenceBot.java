package RGcards.SportsCardProject.bot;

import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Component;

@Component
public class BaseballReferenceBot implements GeneralBot {

    public String getPlayer() {
        WebDriver driver = generateDriver();

        return "";
    }
}
