package com.payrecon.reconciliation.dto;

import com.payrecon.reconciliation.model.ReconciliationRun;

import java.time.Instant;
import java.util.UUID;

// One row in the "list of runs": only the cover page, no result lines (keeps the list small)
public record RunSummary(
        UUID runId,
        UUID statementId,
        Instant runAt,
        int matched,
        int amountMismatch,
        int missingInBank,
        int missingInOurs) {

    public static RunSummary from(ReconciliationRun run) {
        return new RunSummary(run.getId(), run.getStatementId(), run.getRunAt(),
                run.getMatched(), run.getAmountMismatch(), run.getMissingInBank(), run.getMissingInOurs());
    }
}
