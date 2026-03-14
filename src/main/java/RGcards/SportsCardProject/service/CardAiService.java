package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.entity.Card;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardAiService {

    private final GeminiService geminiService;

    public String analyzeCardPotential(Card card) throws JsonProcessingException {
        String prompt = String.format(
            "Analyze the price rise potential of this sports card.\n" +
            "Search up the player's current stats and recent performance to be more accurate.\n" +
            "Player: %s\nYear: %s\nSport: %s\nPublisher: %s\nSet: %s\n" +
            "Auto: %s\nInsert: %s\nParallel: %s\nNumbered: %s\nGrade: %s\nCurrent Value: %s\n\n" +
            "Reply using EXACTLY this format, no deviations:\n" +
            "**Potential Rating:** Low/Medium/High\n\n" +
            "**Key Factors:** 2-3 sentences on what drives the rating.\n\n" +
            "**Main Risk:** 1 sentence on the biggest risk.",
            card.getPlayer(), card.getYear(), card.getSports(), card.getPublisher(), card.getSet(),
            card.getAuto(), card.getInsert(), card.getParallel(), card.getNumbered(),
            card.getGrade(), card.getValue()
        );
        return geminiService.generateContent(prompt, "gemini-2.5-flash", true);
    }
}
