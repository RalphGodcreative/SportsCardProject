package RGcards.SportsCardProject.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bowman_checklists")
public class BowmanChecklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String no;

    private String player;

    private Boolean auto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_id")
    @BatchSize(size = 50)
    private CardSet set;
}
