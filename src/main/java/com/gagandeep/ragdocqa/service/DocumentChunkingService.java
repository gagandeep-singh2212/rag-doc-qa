package com.gagandeep.ragdocqa.service;

import com.gagandeep.ragdocqa.config.RagProperties;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentChunkingService {

    private final RagProperties ragProperties;

    public DocumentChunkingService(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    /**
     * Extracts raw text from a PDF or plain-text upload.
     */
    public String extractText(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (filename.endsWith(".pdf")) {
            try (PDDocument document = Loader.loadPDF(file.getBytes())) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        }
        // Fall back to treating anything else as plain text (.txt, .md, etc.)
        return new String(file.getBytes());
    }

    /**
     * Splits text into overlapping word-count chunks. Overlap prevents a fact
     * from being silently split across two chunks and lost from retrieval.
     */
    public List<String> chunk(String text) {
        String normalized = text.replaceAll("\\s+", " ").trim();
        String[] words = normalized.split(" ");

        int chunkSize = ragProperties.getChunking().getChunkSizeWords();
        int overlap = ragProperties.getChunking().getChunkOverlapWords();
        int step = Math.max(chunkSize - overlap, 1);

        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < words.length; start += step) {
            int end = Math.min(start + chunkSize, words.length);
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < end; i++) {
                sb.append(words[i]).append(' ');
            }
            String chunkText = sb.toString().trim();
            if (!chunkText.isEmpty()) {
                chunks.add(chunkText);
            }
            if (end == words.length) {
                break;
            }
        }
        return chunks;
    }
}
