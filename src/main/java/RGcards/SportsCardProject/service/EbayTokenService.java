package RGcards.SportsCardProject.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
@Slf4j
@RequiredArgsConstructor
public class EbayTokenService {

    @Value("${ebay.client.id}")
    private String clientId;

    @Value("${ebay.client.secret}")
    private String clientSecret;

    @Value("${ebay.api.base-url}")
    private String baseUrl;

    private static final String SCOPE = "https://api.ebay.com/oauth/api_scope";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private String cachedToken;
    private Instant tokenExpiry = Instant.EPOCH;

    public String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }
        return fetchNewToken();
    }

    private String fetchNewToken() {
        String url = baseUrl + "/identity/v1/oauth2/token";

        String credentials = clientId + ":" + clientSecret;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + encoded);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("scope", SCOPE);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        String response;
        try {
            response = restTemplate.postForObject(url, request, String.class);
        } catch (RestClientException e) {
            log.error("Failed to fetch eBay access token: {}", e.getMessage());
            throw e;
        }

        try {
            JsonNode root = objectMapper.readTree(response);
            cachedToken = root.get("access_token").asText();
            int expiresIn = root.get("expires_in").asInt();
            // Subtract 60 seconds as a buffer before actual expiry
            tokenExpiry = Instant.now().plusSeconds(expiresIn - 60);
            log.info("eBay access token refreshed, expires in {}s", expiresIn);
            return cachedToken;
        } catch (Exception e) {
            log.error("Failed to parse eBay token response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse eBay token response", e);
        }
    }
}
