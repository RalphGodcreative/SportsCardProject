package RGcards.SportsCardProject.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EbaySoldItemSummary {

    private String itemId;
    private String title;
    private String price;
    private String currency;
    private String endTime;
    private String itemWebUrl;
    private String imageUrl;

}
