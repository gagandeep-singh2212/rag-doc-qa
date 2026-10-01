package com.gagandeep.ragdocqa.dto;

public class IngestResponse {

    private String documentId;
    private String documentName;
    private int chunkCount;

    public IngestResponse(String documentId, String documentName, int chunkCount) {
        this.documentId = documentId;
        this.documentName = documentName;
        this.chunkCount = chunkCount;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public int getChunkCount() {
        return chunkCount;
    }
}
