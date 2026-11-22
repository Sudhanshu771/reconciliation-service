package com.payrecon.reconciliation.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

// One line of the reconciliation report: one payment and/or one bank line, and the verdict.
@Entity
@Table(name = "reconciliation_results")
public class ReconciliationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID runId;                 // which run this line belongs to

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus status;

    @Column(nullable = false)
    private String reference;

    private UUID expectedPaymentId;     // null when MISSING_IN_OURS
    private String bankTxnId;           // null when MISSING_IN_BANK

    @Column(precision = 19, scale = 2)
    private BigDecimal expectedAmount;  // our amount (null when MISSING_IN_OURS)

    @Column(precision = 19, scale = 2)
    private BigDecimal bankAmount;      // bank amount (null when MISSING_IN_BANK)

    protected ReconciliationResult() { }

    public ReconciliationResult(UUID runId, MatchStatus status, String reference,
                                UUID expectedPaymentId, String bankTxnId,
                                BigDecimal expectedAmount, BigDecimal bankAmount) {
        this.runId = runId;
        this.status = status;
        this.reference = reference;
        this.expectedPaymentId = expectedPaymentId;
        this.bankTxnId = bankTxnId;
        this.expectedAmount = expectedAmount;
        this.bankAmount = bankAmount;
    }

    // bank minus ours: negative = the bank received LESS than we sent
    public BigDecimal getDifference() {
        if (expectedAmount == null || bankAmount == null) {
            return null;
        }
        return bankAmount.subtract(expectedAmount);
    }

    public UUID getId() { return id; }
    public UUID getRunId() { return runId; }
    public MatchStatus getStatus() { return status; }
    public String getReference() { return reference; }
    public UUID getExpectedPaymentId() { return expectedPaymentId; }
    public String getBankTxnId() { return bankTxnId; }
    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public BigDecimal getBankAmount() { return bankAmount; }
}
