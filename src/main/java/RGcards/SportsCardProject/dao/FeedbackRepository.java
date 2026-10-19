package RGcards.SportsCardProject.dao;

import RGcards.SportsCardProject.dto.AdminFeedbackRow;
import RGcards.SportsCardProject.entity.Feedback;
import RGcards.SportsCardProject.enums.FeedbackType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    /** The member's own history on /feedback. */
    List<Feedback> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Admin list; type == null means all types. */
    @Query("""
            SELECT new RGcards.SportsCardProject.dto.AdminFeedbackRow(
                       f.id, f.type, f.content, u.username, u.email, f.createdAt)
            FROM Feedback f LEFT JOIN User u ON u.id = f.userId
            WHERE (:type IS NULL OR f.type = :type)
            ORDER BY f.createdAt DESC
            """)
    List<AdminFeedbackRow> findAdminRows(@Param("type") FeedbackType type);
}
