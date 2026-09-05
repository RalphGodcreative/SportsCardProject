package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dao.CardRepository;
import RGcards.SportsCardProject.dao.SearchKeywordRepository;
import RGcards.SportsCardProject.dao.TagRepository;
import RGcards.SportsCardProject.dao.TransactionInfoRepository;
import RGcards.SportsCardProject.dao.TransactionRepository;
import RGcards.SportsCardProject.dao.UserRepository;
import RGcards.SportsCardProject.entity.Card;
import RGcards.SportsCardProject.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Removes a user and everything they own. Shared by the admin console and by
 * the self-service "delete my account" button on the profile page.
 */
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionInfoRepository transactionInfoRepository;
    private final TagRepository tagRepository;
    private final SearchKeywordRepository searchKeywordRepository;

    @Transactional
    public void deleteUserAndAllData(Long targetId) {
        User target = userRepository.findById(targetId).orElseThrow();
        requireNotLastAdmin(target);

        List<Card> cards = cardRepository.findByUserIdOrderByIdDesc(targetId);
        for (Card card : cards) {
            transactionInfoRepository.deleteAll(transactionInfoRepository.findByCardId(card.getId()));
        }
        cardRepository.deleteAll(cards);
        transactionRepository.deleteAll(transactionRepository.findByUserId(targetId));
        tagRepository.deleteAll(tagRepository.findByUserIdOrderByIdAsc(targetId));
        searchKeywordRepository.deleteAll(searchKeywordRepository.findByUserId(targetId));
        userRepository.deleteById(targetId);
    }

    private void requireNotLastAdmin(User target) {
        if ("ROLE_ADMIN".equals(target.getRole()) && userRepository.countByRole("ROLE_ADMIN") <= 1) {
            throw new IllegalStateException("Cannot remove the last admin");
        }
    }
}
