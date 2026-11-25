package RGcards.SportsCardProject.entity;

import RGcards.SportsCardProject.enums.SellSignalItemStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@Entity
@Table(name = "sell_signal_items")
public class SellSignalItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SellSignalRun run;

    @Column(name = "card_id", nullable = false)
    private Integer cardId;

    /** 1 = best sell candidate; null when not ranked. */
    @Column(name = "rank")
    private Short rank;

    @Enumerated(EnumType.STRING)
    private SellSignalItemStatus status;

    @Column(columnDefinition = "numeric")
    private BigDecimal costBasisTwd;

    private int listingCount;

    @Column(columnDefinition = "numeric")
    private BigDecimal lowAskUsd;

    @Column(columnDefinition = "numeric")
    private BigDecimal medianAskUsd;

    @Column(columnDefinition = "numeric")
    private BigDecimal gainPct;

    @Column(columnDefinition = "numeric")
    private BigDecimal score;

    @Column(length = 255, nullable = false)
    private String ebayQuery;

    /** JSON array of up to 3 {title, price, url}. */
    @Column(length = 4000)
    private String sampleListings;

    @Column(length = 2000)
    private String aiReason;
}
