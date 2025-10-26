package RGcards.SportsCardProject.dto;

import RGcards.SportsCardProject.entity.Card;
import RGcards.SportsCardProject.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionWithCard {
    private Transaction transaction;
    private List<Card> cards;
}
