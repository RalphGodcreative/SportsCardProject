package RGcards.SportsCardProject.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "transaction_infos")
public class TransactionInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private int transactionId;
    private int cardId;
    private String move;

    public TransactionInfo(int transactionId, int cardId, String move) {
        this.transactionId = transactionId;
        this.cardId = cardId;
        this.move = move;
    }
}
