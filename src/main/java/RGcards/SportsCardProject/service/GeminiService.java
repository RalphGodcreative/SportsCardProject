package RGcards.SportsCardProject.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String DEFAULT_MODEL = "gemini-2.0-flash";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public String generateContent(String prompt) throws JsonProcessingException {
        return generateContent(prompt, DEFAULT_MODEL);
    }

    public String generateContent(String prompt, String model) throws JsonProcessingException {
        String url = GEMINI_BASE_URL + model + ":generateContent?key=" + geminiApiKey;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        String response;
        try {
            response = restTemplate.postForObject(url, request, String.class);
        } catch (RestClientException e) {
            log.error("Gemini API request failed for model {}: {}", model, e.getMessage());
            throw e;
        }

        JsonNode root = objectMapper.readTree(response);
        JsonNode candidates = root.get("candidates");

        if (candidates != null && candidates.isArray() && !candidates.isEmpty()) {
            JsonNode parts = candidates.get(0).get("content").get("parts");
            if (parts != null && parts.isArray() && !parts.isEmpty()) {
                return parts.get(0).get("text").asText();
            }
        }

        log.warn("No content returned from Gemini API for model: {}", model);
        return null;
    }
}
