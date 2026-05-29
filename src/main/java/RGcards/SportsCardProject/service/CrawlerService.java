package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.bot.YahooAuctionBot;
import RGcards.SportsCardProject.dao.SearchKeywordRepository;
import RGcards.SportsCardProject.dao.UserRepository;
import RGcards.SportsCardProject.entity.SearchKeyword;
import RGcards.SportsCardProject.entity.SearchProduct;
import RGcards.SportsCardProject.entity.User;
import jakarta.mail.MessagingException;
import org.springframework.transaction.annotation.Transactional;
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
    private EmailService emailService;

    @Autowired
    private SearchKeywordRepository searchKeywordRepository;

    @Autowired
    private UserRepository userRepository;

    public List<SearchProduct> getProductListByKeyword(String keyword) {
        return bot.getNewProductList(keyword, null);
    }

    public List<SearchKeyword> getAllSearchKeyword(Long userId) {
        return searchKeywordRepository.findByUserId(userId);
    }

    public SearchKeyword addKeyword(String keyword, Long userId) {
        if (searchKeywordRepository.findByKeywordAndUserId(keyword, userId) != null) {
            return null;
        }
        SearchKeyword searchKeyword = new SearchKeyword();
        searchKeyword.setKeyword(keyword);
        searchKeyword.setUserId(userId);
        return searchKeywordRepository.save(searchKeyword);
    }

    public boolean deleteKeyword(int id, Long userId) {
        SearchKeyword keyword = searchKeywordRepository.findById(id).orElse(null);
        if (keyword == null || !keyword.getUserId().equals(userId)) {
            return false;
        }
        try {
            searchKeywordRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<SearchProduct> searchResultForKeyword(SearchKeyword searchKeyword) {
        List<SearchProduct> productList = bot.getNewProductList(searchKeyword.getKeyword(), searchKeyword.getLastId());
        LocalDateTime now = LocalDateTime.now();
        searchKeyword.setLastSearchTime(now);
        if (!productList.isEmpty()) {
            searchKeyword.setLastId(productList.get(0).getId());
            searchKeyword.setLastModifyDate(now);
        }
        searchKeywordRepository.save(searchKeyword);
        return productList;
    }

    public SearchKeyword getSearchKeywordByKeyword(String keyword, Long userId) {
        return searchKeywordRepository.findByKeywordAndUserId(keyword, userId);
    }

    @Transactional
    public Map<SearchKeyword, List<SearchProduct>> getResultForUser(Long userId) {
        Map<SearchKeyword, List<SearchProduct>> resultList = new HashMap<>();
        List<SearchKeyword> keywords = searchKeywordRepository.findByUserId(userId);
        for (SearchKeyword keyword : keywords) {
            resultList.put(keyword, searchResultForKeyword(keyword));
        }
        return moveEmptyListsToEnd(resultList);
    }

    @Async
    @Transactional
    public void getResultAsync(Long userId, String toEmail) throws MessagingException {
        Map<SearchKeyword, List<SearchProduct>> resultList = getResultForUser(userId);
        emailService.sendSearchResultEmail(resultList, toEmail);
    }

    @Async
    @Transactional
    public void runCrawlerForAllUsers() throws MessagingException {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            List<SearchKeyword> keywords = searchKeywordRepository.findByUserId(user.getId());
            if (keywords.isEmpty()) continue;
            Map<SearchKeyword, List<SearchProduct>> resultList = getResultForUser(user.getId());
            emailService.sendSearchResultEmail(resultList, user.getEmail());
        }
    }

    public void resetAllKeyword(Long userId) {
        List<SearchKeyword> searchKeywordList = searchKeywordRepository.findByUserId(userId);
        for (SearchKeyword searchKeyword : searchKeywordList) {
            searchKeyword.setLastId(null);
            searchKeyword.setLastSearchTime(null);
            searchKeyword.setLastModifyDate(null);
        }
        searchKeywordRepository.saveAll(searchKeywordList);
    }

    private Map<SearchKeyword, List<SearchProduct>> moveEmptyListsToEnd(
            Map<SearchKeyword, List<SearchProduct>> originalMap) {

        Map<SearchKeyword, List<SearchProduct>> orderedMap = new LinkedHashMap<>();

        for (Map.Entry<SearchKeyword, List<SearchProduct>> entry : originalMap.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                orderedMap.put(entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<SearchKeyword, List<SearchProduct>> entry : originalMap.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                orderedMap.put(entry.getKey(), entry.getValue());
            }
        }

        return orderedMap;
    }
}
