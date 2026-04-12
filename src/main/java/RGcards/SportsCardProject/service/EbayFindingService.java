package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.dto.EbaySoldItemSummary;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EbayFindingService {

    private static final String FINDING_URL = "https://svcs.ebay.com/services/search/FindingService/v1";

    @Value("${ebay.client.id}")
    private String appId;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Search eBay sold (completed) listings by keyword.
     *
     * @param query keyword string (e.g. "1986 Fleer Michael Jordan")
     * @param limit max number of results (1–100)
     * @return list of sold eBay item summaries
     */
    public List<EbaySoldItemSummary> searchSoldItems(String query, int limit) {
        String url = UriComponentsBuilder.fromHttpUrl(FINDING_URL)
                .queryParam("OPERATION-NAME", "findCompletedItems")
                .queryParam("SERVICE-VERSION", "1.0.0")
                .queryParam("SECURITY-APPNAME", appId)
                .queryParam("RESPONSE-DATA-FORMAT", "JSON")
                .queryParam("keywords", query)
                .queryParam("itemFilter(0).name", "SoldItemsOnly")
                .queryParam("itemFilter(0).value", "true")
                .queryParam("paginationInput.entriesPerPage", limit)
                .build()
                .toUriString();

        String response;
        try {
            response = restTemplate.getForObject(url, String.class);
        } catch (RestClientException e) {
            log.error("eBay Finding API request failed for query '{}': {}", query, e.getMessage());
            throw e;
        }

        return parseSoldItems(response);
    }

    private List<EbaySoldItemSummary> parseSoldItems(String response) {
        List<EbaySoldItemSummary> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root
                    .path("findCompletedItemsResponse").get(0)
                    .path("searchResult").get(0)
                    .path("item");

            if (items == null || !items.isArray()) {
                log.warn("No items in eBay Finding API response");
                return results;
            }

            for (JsonNode item : items) {
                EbaySoldItemSummary summary = new EbaySoldItemSummary();
                summary.setItemId(firstText(item, "itemId"));
                summary.setTitle(firstText(item, "title"));
                summary.setItemWebUrl(firstText(item, "viewItemURL"));
                summary.setImageUrl(firstText(item, "galleryURL"));

                JsonNode sellingStatus = item.path("sellingStatus").get(0);
                if (sellingStatus != null) {
                    JsonNode priceNode = sellingStatus.path("currentPrice").get(0);
                    if (priceNode != null) {
                        summary.setPrice(priceNode.path("__value__").asText());
                        summary.setCurrency(priceNode.path("@currencyId").asText());
                    }
                }

                JsonNode listingInfo = item.path("listingInfo").get(0);
                if (listingInfo != null) {
                    summary.setEndTime(firstText(listingInfo, "endTime"));
                }

                results.add(summary);
            }
        } catch (Exception e) {
            log.error("Failed to parse eBay Finding API response: {}", e.getMessage());
        }
        return results;
    }

    private String firstText(JsonNode node, String field) {
        JsonNode arr = node.get(field);
        if (arr != null && arr.isArray() && arr.size() > 0) {
            return arr.get(0).asText();
        }
        return null;
    }
}
