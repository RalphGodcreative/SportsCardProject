package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.entity.SearchKeyword;
import RGcards.SportsCardProject.entity.SearchProduct;
import RGcards.SportsCardProject.entity.User;
import RGcards.SportsCardProject.service.CrawlerService;
import RGcards.SportsCardProject.service.EmailService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    public String CrawlerHome(Model model, @AuthenticationPrincipal User currentUser) {
        List<SearchKeyword> allSearchKeywords = crawlerService.getAllSearchKeyword(currentUser.getId());
        model.addAttribute("searchKeywords", allSearchKeywords);
        return "crawler/keywords";
    }

    @PutMapping("/add")
    @ResponseBody
    public Boolean addKeyword(@RequestParam(name = "keyword") String keyword,
                              @AuthenticationPrincipal User currentUser) {
        SearchKeyword searchKeyword = crawlerService.addKeyword(keyword, currentUser.getId());
        return searchKeyword != null;
    }

    @DeleteMapping("/delete")
    @ResponseBody
    public Boolean delete(@RequestParam(name = "keywordId") int keywordId,
                          @AuthenticationPrincipal User currentUser) {
        return crawlerService.deleteKeyword(keywordId, currentUser.getId());
    }

    @GetMapping("/search-all")
    public String searchAll(Model model, @AuthenticationPrincipal User currentUser) throws MessagingException {
        Map<SearchKeyword, List<SearchProduct>> resultList = crawlerService.getResultForUser(currentUser.getId());
        model.addAttribute("resultList", resultList);
        emailService.sendSearchResultEmail(resultList, currentUser.getEmail());
        return "/crawler/result";
    }

    @ResponseBody
    @GetMapping("/search-all-async")
    public String searchAllAsync(@AuthenticationPrincipal User currentUser) throws MessagingException {
        crawlerService.getResultAsync(currentUser.getId(), currentUser.getEmail());
        return "started the searching process , the email will be sent to you shortly";
    }

    @GetMapping("/rest-all")
    @ResponseBody
    public Boolean resetAll(@AuthenticationPrincipal User currentUser) {
        try {
            crawlerService.resetAllKeyword(currentUser.getId());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
