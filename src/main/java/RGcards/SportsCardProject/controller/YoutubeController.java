package RGcards.SportsCardProject.controller;

import RGcards.SportsCardProject.eto.YoutubeVideo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.SynchronousQueue;

@RequestMapping("/youtube")
@RestController
public class YoutubeController {

    private String youtubeApiKey = "AIzaSyBklRaVTlwPzCd-tSXLYvBnEZDQxO8X-ac";
    private String youtubeUrl = "https://www.googleapis.com/youtube/v3/";

    @GetMapping("/video")
    public List<YoutubeVideo> getVideo() throws JsonProcessingException {
        RestTemplate rt = new RestTemplate();
        Map<String, String> params = new HashMap<>();
        params.put("key", youtubeApiKey);
        params.put("channelId", "UC-3vXiVbUk6eeWA0mde94uQ");
        params.put("part", "snippet");
        params.put("q", "若潼");
        String url = youtubeUrl + "search?" + setUrlForParams(params);
        String result = rt.getForObject(url, String.class, params);
        ObjectMapper om = new ObjectMapper();
        JsonNode items = om.readTree(result).get("items");
        System.out.println(items.size());
        List<YoutubeVideo> res = new ArrayList<>();
        for (JsonNode item : items) {
            String videoId = item.get("id").get("videoId").toString().replaceAll("\"", "");
            String title = item.get("snippet").get("title").toString().replaceAll("\"", "");
            YoutubeVideo video = new YoutubeVideo(videoId, title);
            res.add(video);
            System.out.println(video.toString());

        }


        return res;


    }

    @GetMapping("/keywordVideo")
    public List<YoutubeVideo> getVideoForKeywordInChannel(@RequestParam String channelUrl, @RequestParam String keyword) throws JsonProcessingException {
        String channelId = getChannelId(channelUrl);
        RestTemplate rt = new RestTemplate();
        Map<String, String> params = new HashMap<>();
        params.put("key", youtubeApiKey);
        params.put("channelId", channelId);
        params.put("part", "snippet");
        params.put("q", keyword);
        String url = youtubeUrl + "search?" + setUrlForParams(params);
        String result = rt.getForObject(url, String.class, params);
        ObjectMapper om = new ObjectMapper();
        JsonNode items = om.readTree(result).get("items");
        System.out.println(items.size());
        List<YoutubeVideo> res = new ArrayList<>();
        for (JsonNode item : items) {
            String videoId = item.get("id").get("videoId").toString().replaceAll("\"", "");
            String title = item.get("snippet").get("title").toString().replaceAll("\"", "");
            YoutubeVideo video = new YoutubeVideo(videoId, title);
            res.add(video);
            System.out.println(video.toString());

        }


        return res;


    }

    public String getChannelId(String videoUrl) throws JsonProcessingException {
        String[] urlSplit = videoUrl.split("v=");
        String videoId = urlSplit[1].split("&")[0];
        System.out.println("------------------");
        System.out.println(videoId);
        System.out.println("------------------");
        RestTemplate rt = new RestTemplate();
        Map<String, String> params = new HashMap<>();
        params.put("key", youtubeApiKey);
        params.put("id", videoId);
        params.put("part","snippet");
        String url= youtubeUrl+"videos?"+setUrlForParams(params);
        String result = rt.getForObject(url, String.class, params);
        ObjectMapper om = new ObjectMapper();
        String channeId = om.readTree(result).get("items").get(0).get("snippet").get("channelId").toString().replaceAll("\"", "");
//        String channeId = om.readTree(result).toString();

        return channeId;
    }

    public String setUrlForParams(Map<String, String> params) {

        List<String> keys = new ArrayList<>();
        Set<String> keySets = params.keySet();
        StringBuilder sb = new StringBuilder();
        keys.addAll(keySets);
        for (String key : keys) {
            sb.append(key + "={" + key + "}&");
        }
        return sb.substring(0, sb.length() - 1);
    }

}
