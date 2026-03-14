package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.service.CardAiService;
import RGcards.SportsCardProject.service.CardService;
import RGcards.SportsCardProject.entity.Card;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cardRest")
public class CardRestController {


    @Autowired
    private CardService component;

    @Autowired
    private CardAiService cardAiService;

    @GetMapping("/searchCard")
    public String searchCards(@RequestParam(name = "id", defaultValue = "0") String id, @ModelAttribute("year") String year, @ModelAttribute("publisher") String publisher,
                              @ModelAttribute("set") String set, @ModelAttribute("player") String player,
                              @RequestParam(name = "auto", defaultValue = "false") Boolean auto, @ModelAttribute("insert") String insert,
                              @ModelAttribute("parallel") String parallel, @ModelAttribute("numbered") String numbered,
                              @ModelAttribute("sports") String sports, @ModelAttribute("grade") String grade,
                              @RequestParam(name = "value", defaultValue = "") Double value, @ModelAttribute("note") String note
    ) {
        Card card = new Card(Integer.parseInt(id), year, publisher, set, player, auto, insert, parallel, numbered, sports, grade, value, note);
        List<Card> cards = component.findCardsWithParam(card);
        ObjectMapper om = new ObjectMapper();
        String jsonCards;
        try {
            jsonCards = om.writeValueAsString(cards);
        } catch (JsonProcessingException e) {
            jsonCards = "fail";
            e.printStackTrace();
        }

        return jsonCards;
    }

    @GetMapping("/{id}")
    public String searchCardById(@PathVariable("id") String id){
        Card card = component.getCardById(Integer.parseInt(id));
        String resultCard;
        ObjectMapper om = new ObjectMapper();
        try{
            resultCard=om.writeValueAsString(card);
        }catch (JsonProcessingException e){
            resultCard="fail";
            e.printStackTrace();
        }
        return resultCard;
    }

    @GetMapping("/{id}/potential")
    public String getCardPotential(@PathVariable("id") String id) throws JsonProcessingException {
        Card card = component.getCardById(Integer.parseInt(id));
        ObjectMapper om = new ObjectMapper();
        Map<String, String> response = new HashMap<>();

        if (card == null) {
            response.put("error", "Card not found");
            return om.writeValueAsString(response);
        }

        try {
            response.put("analysis", cardAiService.analyzeCardPotential(card));
        } catch (Exception e) {
            response.put("error", e.getMessage());
        }

        return om.writeValueAsString(response);
    }

    @GetMapping("/deleteCard")
    public String deleteCardById(@RequestParam(name = "cardId") String cardId) throws JsonProcessingException {
        int deleteCount = component.deleteCard(Integer.parseInt(cardId));
        Map<String, Integer> response = new HashMap<>();
        response.put("totalDeleteRow", deleteCount);

        ObjectMapper om = new ObjectMapper();
        return om.writeValueAsString(response);

    }
}
