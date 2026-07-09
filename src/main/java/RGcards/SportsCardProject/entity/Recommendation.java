package RGcards.SportsCardProject.entity;

import RGcards.SportsCardProject.enums.RecommendationAuthor;
import RGcards.SportsCardProject.enums.RecommendationSport;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "recommendations")
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String player;

    @Enumerated(EnumType.STRING)
    private RecommendationSport sport;

    @Column(length = 120)
    private String reason;

    @Enumerated(EnumType.STRING)
    private RecommendationAuthor author;

    private LocalDateTime createdAt;
}
