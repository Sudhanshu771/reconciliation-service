package com.payrecon.reconciliation.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

// One "reconciliation run" = one time we compared a statement with our payments.
// It stores the totals; the line-by-line details are in ReconciliationResult.
@Entity
@Table(name = "reconciliation_runs")
public class ReconciliationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID statementId;          // which uploaded bank statement we checked

    @Column(nullable = false)
    private Instant runAt;

    private int matched;
    private int amountMismatch;
    private int missingInBank;
    private int missingInOurs;

    protected ReconciliationRun() { }

    public ReconciliationRun(UUID statementId) {
        this.statementId = statementId;
        this.runAt = Instant.now();
    }

    // Called once per result line, so the totals are counted as we go
    public void count(MatchStatus status) {
        switch (status) {
            case MATCHED -> matched++;
            case AMOUNT_MISMATCH -> amountMismatch++;
            case MISSING_IN_BANK -> missingInBank++;
            case MISSING_IN_OURS -> missingInOurs++;
        }
    }

    public UUID getId() { return id; }
    public UUID getStatementId() { return statementId; }
    public Instant getRunAt() { return runAt; }
    public int getMatched() { return matched; }
    public int getAmountMismatch() { return amountMismatch; }
    public int getMissingInBank() { return missingInBank; }
    public int getMissingInOurs() { return missingInOurs; }
}
