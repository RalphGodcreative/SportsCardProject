package RGcards.SportsCardProject.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sets")
public class CardSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private Short year;

    private String publisher;

    private String name;
}
