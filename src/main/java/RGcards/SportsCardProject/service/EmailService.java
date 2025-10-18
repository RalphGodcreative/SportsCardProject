package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.entity.SearchKeyword;
import RGcards.SportsCardProject.entity.SearchProduct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
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
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine springTemplateEngine;

    public EmailService(Environment env, JavaMailSender mailSender, SpringTemplateEngine springTemplateEngine) {
        this.env = env;
        this.mailSender = mailSender;
        this.springTemplateEngine = springTemplateEngine;
    }

    /**
     * Sends an email with search results using an HTML template.
     *
     * @param resultList Map of SearchKeyword to list of SearchProduct results
     * @throws MessagingException if sending the email fails
     */
    public void sendSearchResultEmail(Map<SearchKeyword, List<SearchProduct>> resultList) throws MessagingException {
        Map<String, Object> variable = new HashMap<>();
        variable.put("resultList", resultList);
        String mail = buildEmailContent("mail/email-result", variable);
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = today.format(formatter);
        sendHtmlEmail(env.getProperty("app.mail.to"), "Yahoo Auction Search Result " + formattedDate, mail);
    }

    /**
     * Builds the email content from a Thymeleaf template and variables.
     *
     * @param templateName the name of the Thymeleaf template
     * @param variables    the variables to populate the template
     * @return the processed email content as a String
     */
    public String buildEmailContent(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return springTemplateEngine.process(templateName, context);
    }

    /**
     * Sends a simple text email.
     *
     * @param to      recipient email address
     * @param subject email subject
     * @param text    email body text
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

    /**
     * Sends an HTML email asynchronously.
     *
     * @param to          recipient email address
     * @param subject     email subject
     * @param htmlContent HTML content of the email
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            log.info("sending email {} to {}", subject, to);

            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Encountered error sending email", e);
        }
    }
}
