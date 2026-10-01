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

import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingService {

    private final RestTemplate restTemplate;
    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmbeddingService(RestTemplate restTemplate, RagProperties ragProperties) {
        this.restTemplate = restTemplate;
        this.ragProperties = ragProperties;
    }

    /**
     * Embeds a single piece of text (used for the incoming user question).
     */
    public float[] embed(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    /**
     * Embeds many chunks in one HTTP call - far cheaper and faster than
     * calling the API once per chunk during ingestion.
     */
    public List<float[]> embedBatch(List<String> texts) {
        if (ragProperties.getOpenai().getApiKey() == null || ragProperties.getOpenai().getApiKey().isBlank()) {
            throw new RagException(
                "LLM_API_KEY is not set.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(ragProperties.getOpenai().getApiKey());

        var body = new java.util.HashMap<String, Object>();
        body.put("model", ragProperties.getOpenai().getEmbeddingModel());
        body.put("input", texts);

        HttpEntity<Object> request = new HttpEntity<>(body, headers);
        String url = ragProperties.getOpenai().getBaseUrl() + "/embeddings";

        try {
            String rawResponse = restTemplate.postForObject(url, request, String.class);
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode dataArray = root.get("data");

            List<float[]> results = new ArrayList<>();
            for (JsonNode item : dataArray) {
                JsonNode vectorNode = item.get("embedding");
                float[] vector = new float[vectorNode.size()];
                for (int i = 0; i < vectorNode.size(); i++) {
                    vector[i] = (float) vectorNode.get(i).asDouble();
                }
                results.add(vector);
            }
            return results;
        } catch (Exception e) {
            throw new RagException("Failed to generate embeddings: " + e.getMessage(), e);
        }
    }
}
