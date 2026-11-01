package RGcards.SportsCardProject.entity;

import RGcards.SportsCardProject.enums.BowmanSearchType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bowman_search_history")
public class BowmanSearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private BowmanSearchType searchType;

    @Column(length = 100)
    private String player;

    private Short year;

    private String setName;

    /** Null for guests (not logged in) and for deleted users. */
    @Column(name = "user_id")
    private Long userId;

    private LocalDateTime createdAt;
}
