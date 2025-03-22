package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.email.EmailService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@RestController
@RequestMapping("/mail")
public class MailController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private TemplateEngine templateEngine;

    @GetMapping("/")
    public String test() {
        String message = "why";
        try {
            Context context = new Context();
            context.setVariable("message", message);
            templateEngine.clearTemplateCache();
            String mailContent = templateEngine.process("mail/defaultMail", context);
            emailService.sendHtmlEmail("ralphgodtpe@gmail.com", "templateTest", mailContent);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return message;
    }
}
