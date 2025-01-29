package RGcards.SportsCardProject.bot;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class YahooAuctionBot {


    public List<String> getNewProductList(WebDriver driver, String keyword) {
        String url = "https://tw.bid.yahoo.com/search/auction/product?p=" + keyword + "&sort=-ptime";
        driver.get(url);
        try{
            Thread.sleep(8000);
        }catch (InterruptedException e){
            e.printStackTrace();
        }

        List<WebElement> productGridList = driver.findElement(By.className("gridList")).findElements(By.tagName("a"));
        System.out.println("product list size : " + productGridList.size());
        for(int i = 0 ; i<productGridList.size();i++){
            System.out.print(i + " ");
            WebElement title = productGridList.get(i).findElement(By.xpath(".//div/div[2]/span"));
            System.out.println(title.getText());
        }
//        for (WebElement product : productGridList){
//            WebElement title = product.findElement(By.xpath(".//div/div[2]/span"));
//            System.out.println(title.getText());
//        }


            return null;
    }

}
