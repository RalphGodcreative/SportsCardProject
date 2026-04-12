package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dto.EbayItemSummary;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EbayBrowseService {

    @Value("${ebay.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EbayTokenService ebayTokenService;

    /**
     * Search eBay listings by keyword query.
     *
     * @param query   keyword string (e.g. "1986 Fleer Michael Jordan")
     * @param limit   max number of results (1–200)
     * @return list of matching eBay item summaries
     */
    public List<EbayItemSummary> searchItems(String query, int limit) {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/buy/browse/v1/item_summary/search")
                .queryParam("q", query)
                .queryParam("limit", limit)
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + ebayTokenService.getAccessToken());
        headers.set("X-EBAY-C-MARKETPLACE-ID", "EBAY_US");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        String response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, request, String.class).getBody();
        } catch (RestClientException e) {
            log.error("eBay Browse API request failed for query '{}': {}", query, e.getMessage());
            throw e;
        }

        return parseItemSummaries(response);
    }

    private List<EbayItemSummary> parseItemSummaries(String response) {
        List<EbayItemSummary> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.get("itemSummaries");
            if (items == null || !items.isArray()) {
                log.warn("No itemSummaries in eBay response");
                return results;
            }

            for (JsonNode item : items) {
                EbayItemSummary summary = new EbayItemSummary();
                summary.setItemId(textOrNull(item, "itemId"));
                summary.setTitle(textOrNull(item, "title"));
                summary.setCondition(textOrNull(item, "condition"));
                summary.setItemWebUrl(textOrNull(item, "itemWebUrl"));

                JsonNode price = item.get("price");
                if (price != null) {
                    summary.setPrice(textOrNull(price, "value"));
                    summary.setCurrency(textOrNull(price, "currency"));
                }

                JsonNode image = item.get("image");
                if (image != null) {
                    summary.setImageUrl(textOrNull(image, "imageUrl"));
                }

                results.add(summary);
            }
        } catch (Exception e) {
            log.error("Failed to parse eBay item summaries: {}", e.getMessage());
        }
        return results;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null ? value.asText() : null;
    }
}
