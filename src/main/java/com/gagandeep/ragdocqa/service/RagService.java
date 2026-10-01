package com.gagandeep.ragdocqa.service;

import com.gagandeep.ragdocqa.config.RagProperties;
import com.gagandeep.ragdocqa.dto.AskResponse;
import com.gagandeep.ragdocqa.model.DocumentChunk;
import com.gagandeep.ragdocqa.repository.ChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);
    private static final int MAX_SNIPPET_LENGTH = 200;

    private final EmbeddingService embeddingService;
    private final ChunkRepository chunkRepository;
    private final LlmService llmService;
    private final RagProperties ragProperties;

    public RagService(EmbeddingService embeddingService,
                       ChunkRepository chunkRepository,
                       LlmService llmService,
                       RagProperties ragProperties) {
        this.embeddingService = embeddingService;
        this.chunkRepository = chunkRepository;
        this.llmService = llmService;
        this.ragProperties = ragProperties;
    }

    public AskResponse ask(String question, String documentId) {
        long startTime = System.currentTimeMillis();

        float[] queryEmbedding = embeddingService.embed(question);

        long retrievalStart = System.currentTimeMillis();
        List<DocumentChunk> topChunks = chunkRepository.findTopKSimilar(
            queryEmbedding, ragProperties.getRetrieval().getTopK(), documentId);
        long retrievalTimeMs = System.currentTimeMillis() - retrievalStart;

        log.info("Retrieved {} chunks for question '{}' in {}ms", topChunks.size(), question, retrievalTimeMs);

        // Guardrail: nothing relevant found at all, e.g. empty index or unrelated question.
        if (topChunks.isEmpty()) {
            return new AskResponse(
                "I don't have enough information in the provided documents to answer that.",
                List.of(),
                retrievalTimeMs,
                System.currentTimeMillis() - startTime
            );
        }

        List<String> contextTexts = topChunks.stream().map(DocumentChunk::getContent).toList();
        String answer = llmService.generateAnswer(question, contextTexts);

        List<AskResponse.SourceChunk> sources = topChunks.stream()
            .map(c -> new AskResponse.SourceChunk(
                c.getDocumentName(),
                c.getChunkIndex(),
                truncate(c.getContent()),
                Math.round(c.getSimilarityScore() * 10000.0) / 10000.0
            ))
            .toList();

        long totalTimeMs = System.currentTimeMillis() - startTime;
        return new AskResponse(answer, sources, retrievalTimeMs, totalTimeMs);
    }

    private String truncate(String text) {
        if (text.length() <= MAX_SNIPPET_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_SNIPPET_LENGTH) + "...";
    }
}
