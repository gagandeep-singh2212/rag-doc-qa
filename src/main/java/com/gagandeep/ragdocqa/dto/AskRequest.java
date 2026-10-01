package com.gagandeep.ragdocqa.dto;

import jakarta.validation.constraints.NotBlank;

public class AskRequest {

    @NotBlank(message = "question must not be blank")
    private String question;

    // Optional: restrict retrieval to a single previously ingested document.
    private String documentId;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
}
