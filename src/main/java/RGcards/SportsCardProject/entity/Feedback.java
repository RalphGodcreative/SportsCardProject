package RGcards.SportsCardProject.entity;

import RGcards.SportsCardProject.enums.FeedbackType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private FeedbackType type;

    @Column(length = 2000)
    private String content;

    @Column(name = "user_id")
    private Long userId;

    private LocalDateTime createdAt;
}
