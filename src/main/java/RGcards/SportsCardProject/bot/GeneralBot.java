package RGcards.SportsCardProject.bot;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;


public interface GeneralBot {

    default WebDriver generateDriver() {
        System.setProperty("webdriver.chrome.driver", "C:/cd driver/chromedriver.exe");
        ChromeOptions co = new ChromeOptions();
        co.addArguments("--remote-allow-origins=*");
        return new ChromeDriver(co);
    }
}
