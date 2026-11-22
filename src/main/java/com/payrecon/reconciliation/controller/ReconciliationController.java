package com.payrecon.reconciliation.controller;

import com.payrecon.reconciliation.dto.ReconciliationReport;
import com.payrecon.reconciliation.dto.RunSummary;
import com.payrecon.reconciliation.service.ReconciliationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reconciliations")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    // POST /api/v1/reconciliations?statementId=...  → run the matching for that statement
    @PostMapping
    public ResponseEntity<ReconciliationReport> run(@RequestParam UUID statementId) {
        ReconciliationReport report = reconciliationService.reconcile(statementId);
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    // NEW: GET /api/v1/reconciliations  → list of all runs (summaries), newest first
    @GetMapping
    public List<RunSummary> listRuns() {
        return reconciliationService.listRuns();
    }

    // NEW: GET /api/v1/reconciliations/{runId}  → one run with every result line
    @GetMapping("/{runId}")
    public ReconciliationReport getRun(@PathVariable UUID runId) {
        return reconciliationService.getRun(runId);
    }
}
