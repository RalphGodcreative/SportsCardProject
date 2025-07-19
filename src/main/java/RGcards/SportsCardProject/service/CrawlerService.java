package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.bot.YahooAuctionBot;
import RGcards.SportsCardProject.dao.SearchKeywordRepository;
import RGcards.SportsCardProject.entity.SearchKeyword;
import RGcards.SportsCardProject.entity.SearchProduct;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CrawlerService {

    @Autowired
    private YahooAuctionBot bot;

    @Autowired
    private  EmailService emailService;

    @Autowired
    private SearchKeywordRepository searchKeywordRepository;

    public List<SearchProduct> getProductListByKeyword(String keyword) {
        return bot.getNewProductList(keyword);
    }

    public List<SearchKeyword> getAllSearchKeyword() {
        return searchKeywordRepository.findAll();
    }

    public SearchKeyword addKeyword(String keyword) {
        if (searchKeywordRepository.findByKeyword(keyword) != null) {
            return null;
        }
        SearchKeyword searchKeyword = new SearchKeyword();
        searchKeyword.setKeyword(keyword);
        return searchKeywordRepository.save(searchKeyword);
    }

    public boolean deleteKeyword(int id) {
        try {
            searchKeywordRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }


    public List<SearchProduct> searchResultForKeyword(SearchKeyword searchKeyword) {
        List<SearchProduct> productList = bot.getNewProductList(searchKeyword.getKeyword());
        searchKeyword.setLastSearchTime(LocalDateTime.now());
        if (searchKeyword.getLastId() != null) {
            for (int i = 0; i < productList.size(); i++) {
                if (Long.parseLong(productList.get(i).getId())<=Long.parseLong(searchKeyword.getLastId())) {
                    productList = productList.subList(0, i);
                    break;
                }
            }
        }
        if (!productList.isEmpty()) {
            searchKeyword.setLastId(productList.get(0).getId());
        }
        searchKeywordRepository.save(searchKeyword);
        return productList;
    }

    public SearchKeyword getSearchKeywordByKeyword(String keyword) {
        return searchKeywordRepository.findByKeyword(keyword);
    }

    public Map<SearchKeyword, List<SearchProduct>> getResultForAllKeyword() {
        Map<SearchKeyword, List<SearchProduct>> resultList = new HashMap<>();
        List<SearchKeyword> searchKeywordList = searchKeywordRepository.findAll();
        for (SearchKeyword searchKeyword : searchKeywordList) {
            resultList.put(searchKeyword, searchResultForKeyword(searchKeyword));
        }
        return moveEmptyListsToEnd(resultList);
    }

    @Async
    public void getResultAsync() throws MessagingException {
        Map<SearchKeyword, List<SearchProduct>> resultList = getResultForAllKeyword();
        emailService.sendSearchResultEmail(resultList);
    }

    public void resetAllKeyword() {
        List<SearchKeyword> searchKeywordList = searchKeywordRepository.findAll();
        for (SearchKeyword searchKeyword : searchKeywordList) {
            searchKeyword.setLastId(null);
            searchKeyword.setLastSearchTime(null);
        }
        searchKeywordRepository.saveAll(searchKeywordList);
    }

    private Map<SearchKeyword, List<SearchProduct>> moveEmptyListsToEnd(
            Map<SearchKeyword, List<SearchProduct>> originalMap) {

        Map<SearchKeyword, List<SearchProduct>> orderedMap = new LinkedHashMap<>();

        for (Map.Entry<SearchKeyword, List<SearchProduct>> entry : originalMap.entrySet()) {
            List<SearchProduct> list = entry.getValue();
            if (list != null && !list.isEmpty()) {
                orderedMap.put(entry.getKey(), list);
            }
        }

        for (Map.Entry<SearchKeyword, List<SearchProduct>> entry : originalMap.entrySet()) {
            List<SearchProduct> list = entry.getValue();
            if (list == null || list.isEmpty()) {
                orderedMap.put(entry.getKey(), list);
            }
        }

        return orderedMap;
    }
}
