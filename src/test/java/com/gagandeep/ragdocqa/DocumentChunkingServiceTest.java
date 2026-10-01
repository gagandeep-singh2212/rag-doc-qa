package com.gagandeep.ragdocqa;

import com.gagandeep.ragdocqa.config.RagProperties;
import com.gagandeep.ragdocqa.service.DocumentChunkingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class DocumentChunkingServiceTest {

    private DocumentChunkingService chunkingService;

    @BeforeEach
    void setUp() {
        RagProperties props = new RagProperties();
        props.getChunking().setChunkSizeWords(10);
        props.getChunking().setChunkOverlapWords(2);
        chunkingService = new DocumentChunkingService(props);
    }

    @Test
    void chunksShortTextIntoSingleChunk() {
        String text = "This is a short sentence with six words";
        List<String> chunks = chunkingService.chunk(text);

        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0));
    }

    @Test
    void chunksLongTextWithOverlap() {
        // 25 words, chunk size 10, overlap 2 -> step 8 -> expect chunks at 0,8,16,24(partial)
        String text = IntStream.rangeClosed(1, 25)
            .mapToObj(i -> "word" + i)
            .collect(Collectors.joining(" "));

        List<String> chunks = chunkingService.chunk(text);

        assertFalse(chunks.isEmpty());
        // First chunk should start with word1 and contain exactly 10 words
        assertTrue(chunks.get(0).startsWith("word1 "));
        assertEquals(10, chunks.get(0).split(" ").length);

        // Consecutive chunks should overlap: last 2 words of chunk N == first 2 words of chunk N+1
        for (int i = 0; i < chunks.size() - 1; i++) {
            String[] currentWords = chunks.get(i).split(" ");
            String[] nextWords = chunks.get(i + 1).split(" ");
            String currentTail = currentWords[currentWords.length - 2] + " " + currentWords[currentWords.length - 1];
            String nextHead = nextWords[0] + " " + nextWords[1];
            assertEquals(currentTail, nextHead, "Chunk " + i + " and " + (i + 1) + " should overlap by 2 words");
        }
    }

    @Test
    void handlesEmptyTextGracefully() {
        List<String> chunks = chunkingService.chunk("   ");
        assertTrue(chunks.isEmpty());
    }

    @Test
    void normalizesExtraWhitespace() {
        String messyText = "word1    word2\n\nword3\tword4";
        List<String> chunks = chunkingService.chunk(messyText);

        assertEquals(1, chunks.size());
        assertEquals("word1 word2 word3 word4", chunks.get(0));
    }
}
