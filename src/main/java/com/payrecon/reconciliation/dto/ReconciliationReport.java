package com.payrecon.reconciliation.dto;

import com.payrecon.reconciliation.model.MatchStatus;
import com.payrecon.reconciliation.model.ReconciliationResult;
import com.payrecon.reconciliation.model.ReconciliationRun;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// What the API returns: the totals + every result line
public record ReconciliationReport(
        UUID runId,
        UUID statementId,
        Instant runAt,
        int matched,
        int amountMismatch,
        int missingInBank,
        int missingInOurs,
        List<Line> results) {

    public record Line(
            MatchStatus status,
            String reference,
            UUID expectedPaymentId,
            String bankTxnId,
            BigDecimal expectedAmount,
            BigDecimal bankAmount,
            BigDecimal difference) {

        static Line from(ReconciliationResult r) {
            return new Line(r.getStatus(), r.getReference(), r.getExpectedPaymentId(),
                    r.getBankTxnId(), r.getExpectedAmount(), r.getBankAmount(), r.getDifference());
        }
    }

    public static ReconciliationReport from(ReconciliationRun run, List<ReconciliationResult> results) {
        return new ReconciliationReport(
                run.getId(), run.getStatementId(), run.getRunAt(),
                run.getMatched(), run.getAmountMismatch(), run.getMissingInBank(), run.getMissingInOurs(),
                results.stream().map(Line::from).toList());
    }
}
