package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.service.CrawlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/crawler")
public class YahooAuctionController {

    @Autowired
    private CrawlerService crawlerService;

    @GetMapping("/search")
    public String searchForNewProduct(Model model) {
        crawlerService.getProductListByKeyword("岱縈");
        return null;
    }

    @PutMapping("/add")
    @ResponseBody
    public Boolean addKeyword(String keyword) {
        crawlerService.addKeyword(keyword);
        return crawlerService.addKeyword(keyword)!=null;
    }

    @GetMapping("/search-all")
    public String searchAll(Model model){
        crawlerService.getResultForAllKeyword();
        return null;
    }
}
