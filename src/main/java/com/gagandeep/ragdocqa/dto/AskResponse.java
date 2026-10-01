package com.gagandeep.ragdocqa.dto;

import java.util.List;

public class AskResponse {

    private String answer;
    private List<SourceChunk> sources;
    private long retrievalTimeMs;
    private long totalTimeMs;

    public AskResponse(String answer, List<SourceChunk> sources, long retrievalTimeMs, long totalTimeMs) {
        this.answer = answer;
        this.sources = sources;
        this.retrievalTimeMs = retrievalTimeMs;
        this.totalTimeMs = totalTimeMs;
    }

    public String getAnswer() {
        return answer;
    }

    public List<SourceChunk> getSources() {
        return sources;
    }

    public long getRetrievalTimeMs() {
        return retrievalTimeMs;
    }

    public long getTotalTimeMs() {
        return totalTimeMs;
    }

    public static class SourceChunk {
        private String documentName;
        private int chunkIndex;
        private String snippet;
        private double similarityScore;

        public SourceChunk(String documentName, int chunkIndex, String snippet, double similarityScore) {
            this.documentName = documentName;
            this.chunkIndex = chunkIndex;
            this.snippet = snippet;
            this.similarityScore = similarityScore;
        }

        public String getDocumentName() {
            return documentName;
        }

        public int getChunkIndex() {
            return chunkIndex;
        }

        public String getSnippet() {
            return snippet;
        }

        public double getSimilarityScore() {
            return similarityScore;
        }
    }
}
