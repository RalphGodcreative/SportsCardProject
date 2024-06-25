package RGcards.SportsCardProject.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/cheer")
@Controller
public class CheerController {

    @GetMapping("")
    public String def(){
        return "cheerleader";
    }

}
