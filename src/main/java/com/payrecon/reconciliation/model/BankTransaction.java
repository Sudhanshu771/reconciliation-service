package com.payrecon.reconciliation.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bank_transactions")
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;                     // our own id for the row

    @Column(nullable = false)
    private UUID statementId;            // which upload this line came from

    @Column(nullable = false, unique = true)
    private String bankTxnId;            // the bank's id → unique, so no line is saved twice

    @Column(nullable = false)
    private String reference;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private LocalDate valueDate;         // date only: banks report the day, not the time

    private Instant uploadedAt;

    protected BankTransaction() { }

    public BankTransaction(UUID statementId, String bankTxnId, String reference,
                           BigDecimal amount, String currency, LocalDate valueDate) {
        this.statementId = statementId;
        this.bankTxnId = bankTxnId;
        this.reference = reference;
        this.amount = amount;
        this.currency = currency;
        this.valueDate = valueDate;
        this.uploadedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getStatementId() { return statementId; }
    public String getBankTxnId() { return bankTxnId; }
    public String getReference() { return reference; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public LocalDate getValueDate() { return valueDate; }
    public Instant getUploadedAt() { return uploadedAt; }
}