package RGcards.SportsCardProject.eto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleWithCard {
    private Transaction transaction;
    private List<Integer> cardIds;
}
