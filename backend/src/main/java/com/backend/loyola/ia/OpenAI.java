package com.backend.loyola.ia;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenAI {

    private static final Logger log = LoggerFactory.getLogger(OpenAI.class);
    private static final String BASE_URL = "https://integrate.api.nvidia.com/v1";
    private static final String API_KEY = "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";
    private static final String MODEL = "deepseek-ai/deepseek-v4-pro";

    private final OpenAIClient client;

    public OpenAI() {
        this.client = OpenAIOkHttpClient.builder()
                .baseUrl(BASE_URL)
                .apiKey(API_KEY)
                .build();
    }

    public String sendMessage(String userMessage) {
        try {
            log.debug("Sending request to NVIDIA/DeepSeek: model={}", MODEL);

            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .addUserMessage(userMessage)
                    .model(MODEL)
                    .temperature(1.0)
                    .topP(0.95)
                    .maxCompletionTokens(16384)
                    .build();

            ChatCompletion completion = client.chat().completions().create(params);

            String content = completion.choices().get(0).message().content().orElse("");

            log.debug("Received response: {} chars", content.length());
            return content;
        } catch (Exception e) {
            log.error("OpenAI request failed", e);
            throw new RuntimeException("OpenAI request failed: " + e.getMessage());
        }
    }
}
