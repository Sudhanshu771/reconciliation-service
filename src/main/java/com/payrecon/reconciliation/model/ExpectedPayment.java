package com.payrecon.reconciliation.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "expected_payments")
public class ExpectedPayment {

    @Id
    private UUID paymentId;            // SAME id as in payment-service. Not generated here

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private String reference;

    private String payerAccount;
    private String payeeAccount;
    private Instant createdAt;         // when the payment was made
    private Instant receivedAt;        // when WE heard about it from Kafka

    protected ExpectedPayment() { }    // JPA needs this

    public ExpectedPayment(UUID paymentId, BigDecimal amount, String currency, String reference,
                           String payerAccount, String payeeAccount, Instant createdAt) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.currency = currency;
        this.reference = reference;
        this.payerAccount = payerAccount;
        this.payeeAccount = payeeAccount;
        this.createdAt = createdAt;
        this.receivedAt = Instant.now();
    }

    public UUID getPaymentId() { return paymentId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getReference() { return reference; }
    public String getPayerAccount() { return payerAccount; }
    public String getPayeeAccount() { return payeeAccount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReceivedAt() { return receivedAt; }
}