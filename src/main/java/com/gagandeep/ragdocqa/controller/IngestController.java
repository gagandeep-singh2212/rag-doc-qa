package com.gagandeep.ragdocqa.controller;

import com.gagandeep.ragdocqa.dto.IngestResponse;
import com.gagandeep.ragdocqa.exception.RagException;
import com.gagandeep.ragdocqa.service.IngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class IngestController {

    private final IngestionService ingestionService;

    public IngestController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(value = "/ingest", consumes = "multipart/form-data")
    public ResponseEntity<IngestResponse> ingest(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new RagException("Uploaded file is empty");
        }
        IngestResponse response = ingestionService.ingest(file);
        return ResponseEntity.ok(response);
    }
}
