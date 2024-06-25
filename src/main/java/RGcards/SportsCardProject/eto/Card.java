package RGcards.SportsCardProject.eto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Double value;
    private String note;


    public Card(String year, String publisher, String set, String player, Boolean auto, String insert, String parallel, String numbered, String sports, String grade, Double value, String note) {
        this.year = year;
        this.publisher = publisher;
        this.set = set;
        this.player = player;
        this.auto = auto;
        this.insert = insert;
        this.parallel = parallel;
        this.numbered = numbered;
        this.sports = sports;
        this.grade = grade;
        this.value = value;
        this.note = note;
    }

    public Card(String year, String publisher, String set, String player, Boolean auto, String insert, String parallel, String numbered, String sports, String grade, Double value) {
        this.year = year;
        this.publisher = publisher;
        this.set = set;
        this.player = player;
        this.auto = auto;
        this.insert = insert;
        this.parallel = parallel;
        this.numbered = numbered;
        this.sports = sports;
        this.grade = grade;
        this.value = value;
    }
}
