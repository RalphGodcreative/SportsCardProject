package RGcards.SportsCardProject.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private LocalDate date;
    private String type;
    private Double amount;
    private String note;

    public Transaction(LocalDate date, String type, Double amount, String note) {
        this.date = date;
        this.type = type;
        this.amount = amount;
        this.note = note;
    }

    public Transaction(LocalDate date, String type, Double amount) {
        this.date = date;
        this.type = type;
        this.amount = amount;
    }
}
