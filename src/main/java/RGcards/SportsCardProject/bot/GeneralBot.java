package RGcards.SportsCardProject.bot;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;


public interface GeneralBot {

    default WebDriver generateDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions co = new ChromeOptions();
        co.addArguments("--remote-allow-origins=*");
//        co.addArguments("--headless=new");
//        co.addArguments("--no-sandbox");
//        co.addArguments("--disable-dev-shm-usage");
        return new ChromeDriver(co);
    }
}
