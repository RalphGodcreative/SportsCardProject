package RGcards.SportsCardProject.entity;

import RGcards.SportsCardProject.enums.SellSignalAiStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "sell_signal_runs")
public class SellSignalRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private LocalDateTime createdAt;

    private int cardsConsidered;

    /** FX rate used for this run, kept so old runs stay reproducible. */
    @Column(precision = 10, scale = 4)
    private BigDecimal usdToTwd;

    @Enumerated(EnumType.STRING)
    private SellSignalAiStatus aiStatus;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<SellSignalItem> items = new ArrayList<>();

    public void addItem(SellSignalItem item) {
        item.setRun(this);
        items.add(item);
    }
}
