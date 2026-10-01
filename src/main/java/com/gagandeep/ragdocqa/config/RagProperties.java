package com.gagandeep.ragdocqa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private final OpenAi openai = new OpenAi();
    private final Chunking chunking = new Chunking();
    private final Retrieval retrieval = new Retrieval();

    public OpenAi getOpenai() {
        return openai;
    }

    public Chunking getChunking() {
        return chunking;
    }

    public Retrieval getRetrieval() {
        return retrieval;
    }

    public static class OpenAi {
        private String apiKey;
        private String baseUrl;
        private String embeddingModel;
        private int embeddingDimensions;
        private String chatModel;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getEmbeddingModel() {
            return embeddingModel;
        }

        public void setEmbeddingModel(String embeddingModel) {
            this.embeddingModel = embeddingModel;
        }

        public int getEmbeddingDimensions() {
            return embeddingDimensions;
        }

        public void setEmbeddingDimensions(int embeddingDimensions) {
            this.embeddingDimensions = embeddingDimensions;
        }

        public String getChatModel() {
            return chatModel;
        }

        public void setChatModel(String chatModel) {
            this.chatModel = chatModel;
        }
    }

    public static class Chunking {
        private int chunkSizeWords;
        private int chunkOverlapWords;

        public int getChunkSizeWords() {
            return chunkSizeWords;
        }

        public void setChunkSizeWords(int chunkSizeWords) {
            this.chunkSizeWords = chunkSizeWords;
        }

        public int getChunkOverlapWords() {
            return chunkOverlapWords;
        }

        public void setChunkOverlapWords(int chunkOverlapWords) {
            this.chunkOverlapWords = chunkOverlapWords;
        }
    }

    public static class Retrieval {
        private int topK;

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }
    }
}
