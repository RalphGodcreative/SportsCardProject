package RGcards.SportsCardProject.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchProduct {
    private String id;
    private String link;
    private String image;
    private int price;
    private boolean onAuction;
    private String timeLeft;

    public SearchProduct(String id, String link, String image, int price, boolean onAuction) {
        this.id = id;
        this.link = link;
        this.image = image;
        this.price = price;
        this.onAuction = onAuction;
    }
}
