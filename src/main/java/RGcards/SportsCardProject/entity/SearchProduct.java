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
    private String title;
    private String image;
    private int price;
    private boolean onAuction;
    private String timeLeft;

    public SearchProduct(String link, String title, String image, int price, boolean onAuction) {
        this.id = link.split("item/")[1];
        this.link = link;
        this.title = title;
        this.image = image;
        this.price = price;
        this.onAuction = onAuction;
    }
}
