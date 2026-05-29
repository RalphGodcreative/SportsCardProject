package RGcards.SportsCardProject.dao;

import RGcards.SportsCardProject.entity.SearchKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SearchKeywordRepository extends JpaRepository<SearchKeyword, Integer> {

    List<SearchKeyword> findByUserId(Long userId);

    SearchKeyword findByKeywordAndUserId(String keyword, Long userId);
}
