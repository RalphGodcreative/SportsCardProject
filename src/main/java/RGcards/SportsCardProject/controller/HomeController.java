package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.component.CardComponent;
import RGcards.SportsCardProject.eto.Card;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequestMapping("/")
@Controller
@ComponentScan
public class HomeController {

    @GetMapping("/")
    public String randomPage() {
        return "index";
    }


}
