import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.*;

public class GeminiAPI {

    private static String apiKey;
    // **MODIFICATION**: Changed the model name to the correct 'gemini-pro'.
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash-lite:generateContent?key=";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final ConcurrentHashMap<String, String> RESPONSE_CACHE = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService CACHE_CLEANER = Executors.newScheduledThreadPool(1);
    private static volatile long lastApiCallTime = 0;
    private static final long MIN_API_INTERVAL_MS = 1000; // Minimum 1 second between API calls

    private static final String SYSTEM_PROMPT = """
            You are a fast fact-checking AI. For each claim, provide:
            **Claim**: [quote]
            **Rating**: True/Mostly True/Misleading/False/Unverified ('RATING PERCENT%')
            **Reason**: Brief explanation (1-2 sentences)
            **Sources**: Key reference if available
            If no verifiable claims, respond: "No verifiable claims found."
            Be concise and fast.
            """;

    static {
        // Periodically clean the cache to prevent it from growing indefinitely
        CACHE_CLEANER.scheduleAtFixedRate(() -> {
            if (RESPONSE_CACHE.size() > 100) {
                RESPONSE_CACHE.clear();
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

    public static void loadEnvFromFile(String filePath) {
        try {
            Properties props = new Properties();
            props.load(Files.newInputStream(Path.of(filePath)));
            apiKey = props.getProperty("GEMINI_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                throw new RuntimeException("GEMINI_API_KEY not found in the provided env file.");
            }
            System.out.println("API key loaded successfully");
        } catch (Exception e) {
            throw new RuntimeException("Error loading environment file: " + e.getMessage());
        }
    }

    public static CompletableFuture<String> callFactCheckAPIAsync(String transcript) {
        if (transcript == null || transcript.trim().isEmpty()) {
            return CompletableFuture.completedFuture("No content to fact-check.");
        }

        String normalizedTranscript = transcript.trim().toLowerCase();
        if (RESPONSE_CACHE.containsKey(normalizedTranscript)) {
            return CompletableFuture.completedFuture(RESPONSE_CACHE.get(normalizedTranscript));
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceLastCall = currentTime - lastApiCallTime;
        long delayNeeded = 0;

        if (timeSinceLastCall < MIN_API_INTERVAL_MS) {
            delayNeeded = MIN_API_INTERVAL_MS - timeSinceLastCall;
        }
        lastApiCallTime = currentTime + delayNeeded;

        Executor delayedExecutor = CompletableFuture.delayedExecutor(delayNeeded, TimeUnit.MILLISECONDS);

        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = SYSTEM_PROMPT + "\n\nFact-check this:\n" + transcript;
                JSONObject payload = createPayload(prompt);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(API_URL + apiKey))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenApply(response -> {
                            if (response.statusCode() != 200) {
                                return "API Error: HTTP " + response.statusCode() + " - " + response.body();
                            }
                            String result = parseResponse(response.body());
                            if (!result.contains("No verifiable claims")) {
                                RESPONSE_CACHE.put(normalizedTranscript, result);
                            }
                            return result;
                        }).join();

            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, delayedExecutor).exceptionally(e -> {
            System.err.println("API call failed: " + e.getMessage());
            return "API call failed: " + e.getMessage();
        });
    }

    private static JSONObject createPayload(String prompt) {
        JSONObject userMessage = new JSONObject()
                .put("role", "user")
                .put("parts", new JSONArray().put(new JSONObject().put("text", prompt)));

        JSONObject generationConfig = new JSONObject()
                .put("temperature", 0.2)
                .put("maxOutputTokens", 500);

        return new JSONObject()
                .put("contents", new JSONArray().put(userMessage))
                .put("generationConfig", generationConfig);
    }

    private static String parseResponse(String responseBody) {
        try {
            JSONObject jsonResponse = new JSONObject(responseBody);
            if (jsonResponse.has("candidates")) {
                JSONArray candidates = jsonResponse.getJSONArray("candidates");
                if (!candidates.isEmpty()) {
                    JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
                    if (content.has("parts")) {
                        return content.getJSONArray("parts").getJSONObject(0).getString("text").trim();
                    }
                }
            }
            return "Error: No valid response from Gemini API";
        } catch (Exception e) {
            System.err.println("Error parsing API response: " + responseBody);
            return "Error parsing API response: " + e.getMessage();
        }
    }

    public static void shutdown() {
        CACHE_CLEANER.shutdown();
    }
}