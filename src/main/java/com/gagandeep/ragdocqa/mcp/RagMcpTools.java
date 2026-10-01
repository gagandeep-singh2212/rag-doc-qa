package com.gagandeep.ragdocqa.mcp;

import com.gagandeep.ragdocqa.dto.AskResponse;
import com.gagandeep.ragdocqa.service.RagService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP-facing wrapper around RagService. This holds no business logic of its
 * own - it only adapts the existing REST-facing service into the shape an
 * MCP client expects (a plain method with a description Spring AI turns
 * into a JSON schema for the tool).
 */
@Component
public class RagMcpTools {

    private final RagService ragService;

    public RagMcpTools(RagService ragService) {
        this.ragService = ragService;
    }

    @Tool(description = "Answer a question using retrieval-augmented generation over "
        + "previously ingested documents. Returns an answer grounded only in the "
        + "retrieved document content, or a refusal message if nothing relevant is found. "
        + "Use this when the user asks something that should be answered from their own "
        + "uploaded documents rather than general knowledge.")
    public String askDocuments(
        @ToolParam(description = "The natural-language question to answer") String question,
        @ToolParam(description = "Optional document ID to restrict the search to one document. "
            + "Leave empty to search across all ingested documents.", required = false)
        String documentId) {

        AskResponse response = ragService.ask(question, documentId);
        return response.getAnswer();
    }

    @Tool(description = "Answer a question via RAG and also return which document chunks "
        + "were used to generate the answer, with their similarity scores. Use this when "
        + "the caller needs to verify or cite the source of the answer, not just the answer text.")
    public String askDocumentsWithSources(
        @ToolParam(description = "The natural-language question to answer") String question,
        @ToolParam(description = "Optional document ID to restrict the search to one document. "
            + "Leave empty to search across all ingested documents.", required = false)
        String documentId) {

        AskResponse response = ragService.ask(question, documentId);

        StringBuilder result = new StringBuilder();
        result.append("Answer: ").append(response.getAnswer()).append("\n\n");
        result.append("Sources:\n");
        if (response.getSources().isEmpty()) {
            result.append("(none - no relevant chunks were retrieved)\n");
        } else {
            response.getSources().forEach(s -> result.append(String.format(
                "- %s (chunk %d, similarity %.2f): %s%n",
                s.getDocumentName(), s.getChunkIndex(), s.getSimilarityScore(), s.getSnippet())));
        }
        return result.toString();
    }
}
