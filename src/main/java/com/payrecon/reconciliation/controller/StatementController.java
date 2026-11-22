package com.payrecon.reconciliation.controller;

import com.payrecon.reconciliation.dto.StatementUploadResult;
import com.payrecon.reconciliation.service.StatementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
}