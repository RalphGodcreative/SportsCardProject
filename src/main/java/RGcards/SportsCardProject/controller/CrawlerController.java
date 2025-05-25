package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.entity.SearchKeyword;
import RGcards.SportsCardProject.entity.SearchProduct;
import RGcards.SportsCardProject.service.CrawlerService;
import RGcards.SportsCardProject.service.EmailService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/crawler")
public class CrawlerController {

    @Autowired
    private CrawlerService crawlerService;

    @Autowired
    private EmailService emailService;

    @GetMapping
    public String CrawlerHome(Model model){
        List<SearchKeyword> allSearchKeywords = crawlerService.getAllSearchKeyword();
        model.addAttribute("searchKeywords" , allSearchKeywords);
        return "crawler/keywords";
    }

    @GetMapping("/search")
    public String searchForNewProduct(Model model) {
        crawlerService.getProductListByKeyword("岱縈");
        return null;
    }

    @PutMapping("/add")
    @ResponseBody
    public Boolean addKeyword(@RequestParam(name = "keyword") String keyword) {
        SearchKeyword searchKeyword = crawlerService.addKeyword(keyword);
        return searchKeyword != null;
    }
    @DeleteMapping("/delete")
    @ResponseBody
    public Boolean delete(@RequestParam(name = "keywordId") int keywordId){

        return crawlerService.deleteKeyword(keywordId);
    }

//    @GetMapping("/search-all")
//    public String searchAll(Model model) {
//        Map<SearchKeyword,List<SearchProduct>> resultList = crawlerService.getResultForAllKeyword();
//        System.out.println(resultList);
//        model.addAttribute("resultList",resultList);
//        return "/crawler/result";
//    }

    @GetMapping("/search-all")
    public String searchAll(Model model) throws MessagingException {
        Map<SearchKeyword,List<SearchProduct>> resultList = crawlerService.getResultForAllKeyword();
        System.out.println(resultList);
        model.addAttribute("resultList",resultList);
        emailService.sendSearchResultEmail(resultList);
        return "/crawler/result";
    }


    @GetMapping("/rest-all")
    @ResponseBody
    public Boolean resetAll(){
        try{
            crawlerService.resetAllKeyword();
            return true;
        }catch (Exception e){
            return false;
        }
    }

}
