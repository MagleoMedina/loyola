package com.backend.loyola.ia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class IAmodel {

    private static final Logger log = LoggerFactory.getLogger(IAmodel.class);
    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final String API_KEY = "sk-22dc567da18a44bb9f31d2c713bb40ee";

    private final HttpClient client;

    public IAmodel() {
        this.client = HttpClient.newHttpClient();
    }

    public String sendMessage(String systemPrompt, String userMessage) {
        try {
            log.debug("Sending request to DeepSeek: model=deepseek-v4-flash");

            String json = "{\"model\":\"deepseek-v4-flash\",\"messages\":["
                    + "{\"role\":\"system\",\"content\":\"" + escape(systemPrompt) + "\"},"
                    + "{\"role\":\"user\",\"content\":\"" + escape(userMessage) + "\"}"
                    + "],\"thinking\":{\"type\":\"enabled\"},\"reasoning_effort\":\"high\",\"stream\":false}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("DeepSeek API returned {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("API returned status " + response.statusCode());
            }

            String body = response.body();
            String content = extractContent(body);

            log.debug("Received response from DeepSeek ({} chars)", content.length());
            return content;
        } catch (Exception e) {
            log.error("IA request failed", e);
            throw new RuntimeException("IA request failed: " + e.getMessage());
        }
    }

    // ponytail: naive JSON parsing for a fixed response shape
    private String extractContent(String body) {
        String key = "\"content\":\"";
        int start = body.indexOf(key) + key.length();
        StringBuilder content = new StringBuilder();
        for (int i = start; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '\\' && i + 1 < body.length()) {
                char next = body.charAt(i + 1);
                if (next == '"') { content.append('"'); i++; }
                else if (next == 'n') { content.append('\n'); i++; }
                else if (next == '\\') { content.append('\\'); i++; }
                else { content.append(c); }
            } else if (c == '"') {
                break;
            } else {
                content.append(c);
            }
        }
        return content.toString().trim();
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
