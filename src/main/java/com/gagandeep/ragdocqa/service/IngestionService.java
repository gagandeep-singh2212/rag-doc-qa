package com.gagandeep.ragdocqa.service;

import com.gagandeep.ragdocqa.dto.IngestResponse;
import com.gagandeep.ragdocqa.exception.RagException;
import com.gagandeep.ragdocqa.model.DocumentChunk;
import com.gagandeep.ragdocqa.repository.ChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final DocumentChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final ChunkRepository chunkRepository;

    public IngestionService(DocumentChunkingService chunkingService,
                             EmbeddingService embeddingService,
                             ChunkRepository chunkRepository) {
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.chunkRepository = chunkRepository;
    }

    public IngestResponse ingest(MultipartFile file) {
        String documentId = UUID.randomUUID().toString();
        String documentName = file.getOriginalFilename() != null ? file.getOriginalFilename() : documentId;

        String text;
        try {
            text = chunkingService.extractText(file);
        } catch (IOException e) {
            throw new RagException("Could not read uploaded file: " + e.getMessage(), e);
        }

        if (text == null || text.isBlank()) {
            throw new RagException("Uploaded document contains no extractable text");
        }

        List<String> textChunks = chunkingService.chunk(text);
        log.info("Document '{}' split into {} chunks", documentName, textChunks.size());

        List<float[]> embeddings = embeddingService.embedBatch(textChunks);

        List<DocumentChunk> chunksToSave = new ArrayList<>();
        for (int i = 0; i < textChunks.size(); i++) {
            chunksToSave.add(new DocumentChunk(documentId, documentName, i, textChunks.get(i), embeddings.get(i)));
        }
        chunkRepository.saveAll(chunksToSave);

        return new IngestResponse(documentId, documentName, chunksToSave.size());
    }
}
