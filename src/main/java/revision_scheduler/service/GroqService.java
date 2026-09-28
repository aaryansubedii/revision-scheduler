package revision_scheduler.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GroqService {

    public record TopicSuggestion(String name, int difficulty, double estimatedHours) {}

    @Value("${groq.api.key}")
    private String apiKey;

    private final RestClient restClient = RestClient.create();
    private final JsonMapper mapper = JsonMapper.builder().build();

    public List<TopicSuggestion> suggestTopics(String moduleName) {
        String prompt = "You are a university study advisor. Break the module \"" + moduleName
                + "\" into 5 to 8 specific revision topics. For each topic give a difficulty from 1 (easy) to 5 (hard) "
                + "and a realistic number of revision hours needed. "
                + "Respond with ONLY a JSON array, no other text, in exactly this format: "
                + "[{\"name\": \"Topic name\", \"difficulty\": 3, \"estimatedHours\": 4}]";

        Map<String, Object> body = Map.of(
                "model", "openai/gpt-oss-120b",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.2
        );

        String raw = restClient.post()
                .uri("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode root = mapper.readTree(raw);
        String content = root.path("choices").path(0).path("message").path("content").textValue();

        // The model sometimes wraps JSON in extra text, so grab just the array part
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start == -1 || end == -1) {
            throw new RuntimeException("AI response did not contain a topic list");
        }

        JsonNode array = mapper.readTree(content.substring(start, end + 1));
        List<TopicSuggestion> result = new ArrayList<>();
        for (JsonNode n : array) {
            int difficulty = Math.max(1, Math.min(5, n.path("difficulty").asInt(3)));
            result.add(new TopicSuggestion(
                    n.path("name").textValue(),
                    difficulty,
                    n.path("estimatedHours").asDouble(4)
            ));
        }
        return result;
    }
}