package RGcards.SportsCardProject.dto;

import RGcards.SportsCardProject.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserRow {
    private User user;
    private int cardCount;
    private int keywordCount;
    private int effectiveMaxAiCalls;
}
