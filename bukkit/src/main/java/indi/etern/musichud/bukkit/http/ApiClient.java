package indi.etern.musichud.bukkit.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.StringJoiner;

public class ApiClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .version(HttpClient.Version.HTTP_2)
            .build();

    public static JsonNode get(String baseUrl, String path, Map<String, String> query, String cookie, int timeoutMs) {
        try {
            URI uri = buildUri(baseUrl, path, query);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .GET();
            applyCookie(builder, cookie);
            HttpResponse<String> response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return MAPPER.readTree(response.body());
        } catch (Exception e) {
            throw new RuntimeException("GET " + path + " failed", e);
        }
    }

    public static JsonNode post(String baseUrl, String path, Map<String, String> query, JsonNode body, String cookie, int timeoutMs) {
        try {
            URI uri = buildUri(baseUrl, path, query);
            ObjectNode payload = body != null ? (body.isObject() ? (ObjectNode) body : MAPPER.createObjectNode()) : MAPPER.createObjectNode();
            if (cookie == null || cookie.isBlank()) {
                payload.put("noCookie", true);
            }
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8));
            applyCookie(builder, cookie);
            HttpResponse<String> response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return MAPPER.readTree(response.body());
        } catch (Exception e) {
            throw new RuntimeException("POST " + path + " failed", e);
        }
    }

    private static URI buildUri(String baseUrl, String path, Map<String, String> query) {
        String url = baseUrl + path;
        if (query != null && !query.isEmpty()) {
            StringJoiner joiner = new StringJoiner("&");
            for (Map.Entry<String, String> entry : query.entrySet()) {
                String key = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
                String value = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
                joiner.add(key + "=" + value);
            }
            url += "?" + joiner;
        }
        return URI.create(url);
    }

    private static void applyCookie(HttpRequest.Builder builder, String rawCookie) {
        String normalized = normalizeCookie(rawCookie);
        if (normalized != null && !normalized.isBlank()) {
            builder.header("Cookie", normalized);
        }
    }

    public static String normalizeCookie(String rawCookie) {
        if (rawCookie == null) {
            return null;
        }
        String cookie = rawCookie.trim();
        if (cookie.isEmpty()) {
            return null;
        }
        StringJoiner joiner = new StringJoiner("; ");
        String[] parts = cookie.contains(";;") ? cookie.split(";;") : new String[]{cookie};
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String pair = trimmed.split(";", 2)[0].trim();
            if (pair.contains("=")) {
                joiner.add(pair);
            }
        }
        String result = joiner.toString();
        return result.isEmpty() ? null : result;
    }
}
