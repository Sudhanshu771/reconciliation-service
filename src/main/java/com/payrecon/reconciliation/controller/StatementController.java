package com.payrecon.reconciliation.controller;

import com.payrecon.reconciliation.dto.StatementSummary;
import com.payrecon.reconciliation.dto.StatementUploadResult;
import com.payrecon.reconciliation.service.StatementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/statements")
public class StatementController {

    private final StatementService statementService;

    public StatementController(StatementService statementService) {
        this.statementService = statementService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StatementUploadResult> upload(@RequestParam("file") MultipartFile file) {
        StatementUploadResult result = statementService.upload(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // NEW: GET /api/v1/statements → every uploaded statement, newest first
    @GetMapping
    public List<StatementSummary> list() {
        return statementService.list();
    }
}