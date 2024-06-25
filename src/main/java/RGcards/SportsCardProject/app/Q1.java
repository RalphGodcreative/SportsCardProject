package RGcards.SportsCardProject.app;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

public class Q1 {
    public static void main(String[] args) {
        String webhookUrl = "https://wavedev01-dev-ed.my.salesforce-sites.com/exam/services/apexrest/HelloWorld";

        String payload = "{\"message\": \"" + "HI" + "\"}";

        RestTemplate restTemplate = new RestTemplate();

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create HttpEntity with headers and payload
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        // Make POST request to webhook URL
        ResponseEntity<String> response = restTemplate.exchange(webhookUrl, HttpMethod.POST, entity, String.class);

        // Get response status code
        System.out.println(response.getBody());
    }


}
