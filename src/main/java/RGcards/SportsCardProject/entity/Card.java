package RGcards.SportsCardProject.entity;

import RGcards.SportsCardProject.util.DataProcessUtil;
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
