package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dao.FeedbackRepository;
import RGcards.SportsCardProject.dto.AdminFeedbackRow;
import RGcards.SportsCardProject.entity.Feedback;
import RGcards.SportsCardProject.enums.FeedbackType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    static final int MAX_CONTENT_LENGTH = 2000;

    private final FeedbackRepository feedbackRepository;

    public Feedback submit(Long userId, FeedbackType type, String content) {
        String trimmed = content == null ? "" : content.trim();
        if (type == null) {
            throw new IllegalArgumentException("請選擇類型");
        }
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("內容不可為空");
        }
        if (trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("內容最多 " + MAX_CONTENT_LENGTH + " 字");
        }
        Feedback feedback = new Feedback();
        feedback.setType(type);
        feedback.setContent(trimmed);
        feedback.setUserId(userId);
        feedback.setCreatedAt(LocalDateTime.now());
        return feedbackRepository.save(feedback);
    }

    public List<Feedback> findForUser(Long userId) {
        return feedbackRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<AdminFeedbackRow> findAllForAdmin(FeedbackType type) {
        return feedbackRepository.findAdminRows(type);
    }
}
