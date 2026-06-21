package com.backend.loyola.ia;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Gemini {

    private static final Logger log = LoggerFactory.getLogger(Gemini.class);
    private static final String API_KEY = "AIzaSyBBCNNHIJXrQXLIF8i1pQbbmOK9mYyQPZI";
    private static final String MODEL = "gemini-3-flash-preview";

    private final Client client;

    public Gemini() {
        this.client = Client.builder().apiKey(API_KEY).build();
    }

    public String sendMessage(String systemPrompt, String userMessage) {
        try {
            log.debug("Sending request to Gemini: model={}", MODEL);

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .systemInstruction(Content.builder()
                            .parts(List.of(Part.builder()
                                    .text(systemPrompt)
                                    .build()))
                            .build())
                    .build();

            GenerateContentResponse response = client.models.generateContent(
                    MODEL, userMessage, config);

            String content = response.candidates()
                    .orElseThrow()
                    .get(0).content()
                    .orElseThrow()
                    .parts()
                    .orElseThrow()
                    .get(0).text()
                    .orElseThrow();

            log.debug("Received response from Gemini ({} chars)", content.length());
            return content;
        } catch (Exception e) {
            log.error("Gemini request failed", e);
            throw new RuntimeException("Gemini request failed: " + e.getMessage());
        }
    }
}
