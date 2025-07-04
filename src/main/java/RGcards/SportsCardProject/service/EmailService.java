package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.entity.SearchKeyword;
import RGcards.SportsCardProject.entity.SearchProduct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    private final Environment env;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine springTemplateEngine;

    public EmailService(Environment env) {
        this.env = env;
    }

    public void sendSearchResultEmail(Map<SearchKeyword, List<SearchProduct>> resultList) throws MessagingException {
        Map<String, Object> variable = new HashMap<>();
        variable.put("resultList", resultList);
        String mail = buildEmailContent("mail/email-result", variable);
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = today.format(formatter);
        sendHtmlEmail(env.getProperty("app.mail.to"), "Yahoo Auction Search Result " + formattedDate, mail);
    }

    public String buildEmailContent(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return springTemplateEngine.process(templateName, context);
    }


    public void sendSimpleEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
//            System.out.println(htmlContent);
            log.info("sending email {} to {}", subject, to);

            mailSender.send(message);
        } catch (Exception e) {
            log.error("encounter error sending email");
            e.printStackTrace();
        }

    }
}
