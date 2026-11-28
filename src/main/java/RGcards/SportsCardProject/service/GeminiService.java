package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.enums.GeminiModel;
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
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AppSettingService appSettingService;

    public String generateContent(String prompt) throws JsonProcessingException {
        return generateContent(prompt, false);
    }

    /**
     * Calls the model chosen in admin settings. Search grounding is requested only when
     * that model has it on the free tier; otherwise the prompt runs ungrounded.
     */
    public String generateContent(String prompt, boolean wantSearch) throws JsonProcessingException {
        GeminiModel selected = appSettingService.getGeminiModel();
        String model = selected.getId();
        boolean useSearch = wantSearch && selected.isFreeSearchGrounding();
        if (wantSearch && !useSearch) {
            log.debug("Model {} has no free search grounding; sending without google_search", model);
        }
        String url = GEMINI_BASE_URL + model + ":generateContent?key=" + geminiApiKey;

        List<Map<String, Object>> contents = List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))
        );

        Map<String, Object> requestBody;
        if (useSearch) {
            requestBody = Map.of(
                    "contents", contents,
                    "tools", List.of(Map.of("google_search", Map.of()))
            );
        } else {
            requestBody = Map.of("contents", contents);
        }

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
            // Grounded (google_search) answers can come back split over several parts.
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (parts.isArray() && !parts.isEmpty()) {
                StringBuilder text = new StringBuilder();
                for (JsonNode part : parts) {
                    JsonNode t = part.get("text");
                    if (t != null && !t.isNull()) {
                        text.append(t.asText());
                    }
                }
                if (!text.isEmpty()) {
                    return text.toString();
                }
            }
        }

        log.warn("No content returned from Gemini API for model: {}", model);
        return null;
    }
}
