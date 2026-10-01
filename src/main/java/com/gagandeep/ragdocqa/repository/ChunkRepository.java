package com.gagandeep.ragdocqa.repository;

import com.gagandeep.ragdocqa.model.DocumentChunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class ChunkRepository {

    private final JdbcTemplate jdbcTemplate;

    public ChunkRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(DocumentChunk chunk) {
        jdbcTemplate.update(
            "INSERT INTO document_chunk (document_id, document_name, chunk_index, content, embedding) " +
                "VALUES (?, ?, ?, ?, ?::vector)",
            chunk.getDocumentId(),
            chunk.getDocumentName(),
            chunk.getChunkIndex(),
            chunk.getContent(),
            toVectorLiteral(chunk.getEmbedding())
        );
    }

    public void saveAll(List<DocumentChunk> chunks) {
        List<Object[]> batchArgs = chunks.stream()
            .map(c -> new Object[]{
                c.getDocumentId(), c.getDocumentName(), c.getChunkIndex(),
                c.getContent(), toVectorLiteral(c.getEmbedding())
            })
            .toList();

        jdbcTemplate.batchUpdate(
            "INSERT INTO document_chunk (document_id, document_name, chunk_index, content, embedding) " +
                "VALUES (?, ?, ?, ?, ?::vector)",
            batchArgs
        );
    }

    /**
     * Cosine-distance nearest-neighbour search using pgvector's "<=>" operator.
     * pgvector returns distance (0 = identical), so similarity = 1 - distance.
     * Optionally scoped to a single document via documentId.
     */
    public List<DocumentChunk> findTopKSimilar(float[] queryEmbedding, int topK, String documentId) {
        String vectorLiteral = toVectorLiteral(queryEmbedding);

        String sql = "SELECT id, document_id, document_name, chunk_index, content, created_at, " +
            "1 - (embedding <=> ?::vector) AS similarity " +
            "FROM document_chunk " +
            (documentId != null && !documentId.isBlank() ? "WHERE document_id = ? " : "") +
            "ORDER BY embedding <=> ?::vector " +
            "LIMIT ?";

        Object[] args = (documentId != null && !documentId.isBlank())
            ? new Object[]{vectorLiteral, documentId, vectorLiteral, topK}
            : new Object[]{vectorLiteral, vectorLiteral, topK};

        return jdbcTemplate.query(sql, args, (rs, rowNum) -> {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setId(rs.getLong("id"));
            chunk.setDocumentId(rs.getString("document_id"));
            chunk.setDocumentName(rs.getString("document_name"));
            chunk.setChunkIndex(rs.getInt("chunk_index"));
            chunk.setContent(rs.getString("content"));
            Timestamp ts = rs.getTimestamp("created_at");
            if (ts != null) {
                chunk.setCreatedAt(ts.toLocalDateTime());
            }
            chunk.setSimilarityScore(rs.getDouble("similarity"));
            return chunk;
        });
    }

    /** pgvector accepts vectors as a string literal like "[0.1,0.2,0.3]". */
    private String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) {
                sb.append(',');
            }
        }
        sb.append(']');
        return sb.toString();
    }
}
