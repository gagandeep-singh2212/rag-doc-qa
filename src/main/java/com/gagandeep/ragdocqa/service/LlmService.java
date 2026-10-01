package com.gagandeep.ragdocqa.service;

import com.gagandeep.ragdocqa.config.RagProperties;
import com.gagandeep.ragdocqa.exception.RagException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class LlmService {

    private static final String SYSTEM_PROMPT = """
        You are a precise assistant answering questions using ONLY the provided context.
        If the context does not contain enough information to answer confidently,
        respond exactly with: "I don't have enough information in the provided documents to answer that."
        Never use outside knowledge. Cite which context snippet(s) you used when relevant.
        """;

    private final RestTemplate restTemplate;
    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LlmService(RestTemplate restTemplate, RagProperties ragProperties) {
        this.restTemplate = restTemplate;
        this.ragProperties = ragProperties;
    }

    public String generateAnswer(String question, List<String> contextChunks) {
        if (ragProperties.getOpenai().getApiKey() == null || ragProperties.getOpenai().getApiKey().isBlank()) {
            throw new RagException(
                "LLM_API_KEY is not set.");
        }

        StringBuilder contextBlock = new StringBuilder();
        for (int i = 0; i < contextChunks.size(); i++) {
            contextBlock.append("[Snippet ").append(i + 1).append("]\n")
                .append(contextChunks.get(i)).append("\n\n");
        }

        String userPrompt = "Context:\n" + contextBlock + "\nQuestion: " + question;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(ragProperties.getOpenai().getApiKey());

        Map<String, Object> body = Map.of(
            "model", ragProperties.getOpenai().getChatModel(),
            "messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userPrompt)
            ),
            "temperature", 0.1
        );

        HttpEntity<Object> request = new HttpEntity<>(body, headers);
        String url = ragProperties.getOpenai().getBaseUrl() + "/chat/completions";

        try {
            String rawResponse = restTemplate.postForObject(url, request, String.class);
            JsonNode root = objectMapper.readTree(rawResponse);
            return root.get("choices").get(0).get("message").get("content").asText();
        } catch (Exception e) {
            throw new RagException("Failed to generate answer from LLM: " + e.getMessage(), e);
        }
    }
}
