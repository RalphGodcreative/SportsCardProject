package RGcards.SportsCardProject.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequestMapping("/")
@Controller
public class RandomizerController {

    @GetMapping("/random")
    public String randomPage() {
        return "randomPage";
    }

    @PostMapping("/randomize")
    public String randomList(@ModelAttribute("nameList") String nameList, @ModelAttribute("randomTimes") String randomTimes, Model model) {

        List<String> names = new ArrayList<>(Arrays.asList(nameList.split("\\r?\\n")));

        List<String> randomList = randomizeList(names);
        int rt = Integer.parseInt(randomTimes) + 1;
        model.addAttribute("nameList", nameList);
        model.addAttribute("randomList", randomList);
        model.addAttribute("randomTimes", rt);

        return "randomResult";
    }

    public List<String> randomizeList(List<String> list) {
        for (int i = 0; i < list.size(); i++) {
            int x = (int) (Math.random() * list.size());
            String temp = list.get(i);
            list.set(i, list.get(x));
            list.set(x, temp);
        }
        return list;
    }


}
