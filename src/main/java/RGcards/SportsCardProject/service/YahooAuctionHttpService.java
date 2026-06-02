package RGcards.SportsCardProject.service;

import RGcards.SportsCardProject.entity.SearchProduct;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class YahooAuctionHttpService {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public List<SearchProduct> getNewProductList(String keyword, String lastId) {
        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url = "https://tw.bid.yahoo.com/search/auction/product?p=" + encodedKeyword + "&sort=-ptime";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return parseSearchPage(response.body(), lastId);

        } catch (Exception e) {
            log.error("Failed to fetch Yahoo Auction products for keyword: {}", keyword, e);
            return new ArrayList<>();
        }
    }

    private List<SearchProduct> parseSearchPage(String html, String lastId) throws Exception {
        String marker = "id=\"isoredux-data\" type=\"mime/invalid\">";
        int start = html.indexOf(marker);
        if (start == -1) {
            log.warn("Could not find isoredux-data script tag in Yahoo Auction response");
            return new ArrayList<>();
        }
        start += marker.length();
        int end = html.indexOf("</script>", start);
        if (end == -1) {
            log.warn("Could not find closing </script> tag for isoredux-data");
            return new ArrayList<>();
        }
        String json = html.substring(start, end).trim();

        JsonNode hits = mapper.readTree(json)
                .path("search").path("ecsearch").path("hits");

        List<SearchProduct> products = new ArrayList<>();
        long now = System.currentTimeMillis() / 1000;
        int i = 0;

        for (JsonNode hit : hits) {
            String itemUrl = hit.path("ec_item_url").asText();
            if (!itemUrl.contains("/item/")) {
                continue;
            }

            String productId = hit.path("ec_productid").asText();
            if (lastId != null && !productId.isEmpty()
                    && Long.parseLong(productId) <= Long.parseLong(lastId)) {
                break;
            }

            boolean onAuction = !hit.path("ec_numbids").asText().isEmpty();
            double price = onAuction
                    ? hit.path("ec_price").asDouble()
                    : hit.path("ec_buyprice").asDouble();

            SearchProduct product = new SearchProduct();
            product.setId(productId);
            product.setLink(itemUrl);
            product.setTitle(hit.path("ec_title").asText());
            product.setImage(hit.path("ec_image").asText());
            product.setPrice((int) price);
            product.setOnAuction(onAuction);

            if (onAuction) {
                long remaining = hit.path("ec_endtime").asLong() - now;
                if (remaining > 0) {
                    long days = remaining / 86400;
                    long hours = (remaining % 86400) / 3600;
                    product.setTimeLeft(days > 0 ? "剩" + days + "天" : "剩" + hours + "小時");
                }
            }

            log.info("{} {}", i, product);
            i++;
            products.add(product);
        }

        log.info("product list size : {}", products.size());
        return products;
    }
}
