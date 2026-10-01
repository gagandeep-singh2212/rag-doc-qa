package com.gagandeep.ragdocqa.controller;

import com.gagandeep.ragdocqa.dto.AskRequest;
import com.gagandeep.ragdocqa.dto.AskResponse;
import com.gagandeep.ragdocqa.service.RagService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class QueryController {

    private final RagService ragService;

    public QueryController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    public ResponseEntity<AskResponse> ask(@Valid @RequestBody AskRequest request) {
        AskResponse response = ragService.ask(request.getQuestion(), request.getDocumentId());
        return ResponseEntity.ok(response);
    }
}
