package RGcards.SportsCardProject.bot;

import RGcards.SportsCardProject.entity.SearchProduct;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class YahooAuctionBot implements GeneralBot{


    public List<SearchProduct> getNewProductList(String keyword) {
        WebDriver driver = generateDriver();
        String url = "https://tw.bid.yahoo.com/search/auction/product?p=" + keyword + "&sort=-ptime";
        driver.get(url);
        try{
            Thread.sleep(8000);
        }catch (InterruptedException e){
            e.printStackTrace();
        }

        List<WebElement> productGridList = driver.findElement(By.className("gridList")).findElements(By.tagName("a"));
        List<SearchProduct> searchProductList = new ArrayList<>();
        System.out.println("product list size : " + productGridList.size());
        for(int i = 0 ; i<productGridList.size();i++){
            System.out.print(i + " ");
            try {
                SearchProduct searchProduct =  fetchProduct(productGridList.get(i));
                searchProductList.add(searchProduct);
            }catch (Exception e){
                System.out.println("the "+i +"th encounter error");
                e.printStackTrace();
            }
        }
        System.out.println(searchProductList);
            driver.close();
            return searchProductList;
    }

    private SearchProduct fetchProduct(WebElement element){
        String link = element.getAttribute("href");
        String id = link.split("item/")[1];
        String image = element.findElement(By.tagName("img")).getAttribute("src");
        String title = element.findElement(By.xpath(".//div/div[2]/span")).getText();
        String priceTag =  element.findElement(By.xpath(".//div/div[2]/div[2]/span")).getText().substring(1);
        int price = Integer.parseInt(priceTag.replace(",", ""));
        Boolean auction = !element.findElements(By.xpath(".//div/div[2]/div[4]/div[1]")).isEmpty();
        SearchProduct searchProduct = new SearchProduct(id,link,image,price,auction);
        if(auction){
            searchProduct.setTimeLeft(element.findElement(By.xpath(".//div/div[2]/div[4]/div[1]/span[2]")).getText());
        }
        return searchProduct;
    }

}
