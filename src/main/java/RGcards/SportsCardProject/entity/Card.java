package RGcards.SportsCardProject.entity;

import RGcards.SportsCardProject.enums.CardType;
import RGcards.SportsCardProject.util.DataProcessUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String year;
    private String publisher;
    private String set;
    private String player;
    private Boolean auto;
    private String insert;
    private String parallel;
    private String numbered;
    private String sports;
    private String grade;
    @Column(columnDefinition = "numeric")
    private Double value;
    private String note;

    @Enumerated(EnumType.STRING)
    private CardType type;

    @Column(name = "user_id")
    private Long userId;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "card_tags",
            joinColumns = @JoinColumn(name = "card_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @OrderBy("name")
    @BatchSize(size = 50)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Tag> tags = new LinkedHashSet<>();

    /**
     * Inbound only. JSON card payloads carry bare tag ids; controllers resolve them
     * through TagService (which drops ids the user doesn't own) into {@link #tags}.
     * Never serialized — outbound JSON exposes {@code tags} instead.
     */
    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private List<Long> tagIds;


    public Card(int id, String year, String publisher, String set, String player, Boolean auto, String insert, String parallel, String numbered, String sports, String grade, Double value, String note) {
        this.id = id;
        this.year = year;
        this.publisher = DataProcessUtil.upperCaseFirstLetter(publisher);
        this.set = DataProcessUtil.upperCaseFirstLetter(set);
        this.player = DataProcessUtil.upperCaseFirstLetter(player);
        this.auto = auto;
        this.insert = DataProcessUtil.upperCaseFirstLetter(insert);
        this.parallel = DataProcessUtil.upperCaseFirstLetter(parallel);
        this.numbered = numbered;
        this.sports = DataProcessUtil.upperCaseFirstLetter(sports);
        this.grade = grade;
        this.value = value;
        this.note = note;
    }

    public Card(String year, String publisher, String set, String player, Boolean auto, String insert, String parallel, String numbered, String sports, String grade, Double value, String note) {
        this.year = year;
        this.publisher = DataProcessUtil.upperCaseFirstLetter(publisher);
        this.set = DataProcessUtil.upperCaseFirstLetter(set);
        this.player = DataProcessUtil.upperCaseFirstLetter(player);
        this.auto = auto;
        this.insert = DataProcessUtil.upperCaseFirstLetter(insert);
        this.parallel = DataProcessUtil.upperCaseFirstLetter(parallel);
        this.numbered = numbered;
        this.sports = DataProcessUtil.upperCaseFirstLetter(sports);
        this.grade = grade;
        this.value = value;
        this.note = note;
    }

    public Card(String year, String publisher, String set, String player, Boolean auto, String insert, String parallel, String numbered, String sports, String grade, Double value) {
        this.year = year;
        this.publisher = DataProcessUtil.upperCaseFirstLetter(publisher);
        this.set = DataProcessUtil.upperCaseFirstLetter(set);
        this.player = DataProcessUtil.upperCaseFirstLetter(player);
        this.auto = auto;
        this.insert = DataProcessUtil.upperCaseFirstLetter(insert);
        this.parallel = DataProcessUtil.upperCaseFirstLetter(parallel);
        this.numbered = numbered;
        this.sports = DataProcessUtil.upperCaseFirstLetter(sports);
        this.grade = grade;
        this.value = value;
    }
}
