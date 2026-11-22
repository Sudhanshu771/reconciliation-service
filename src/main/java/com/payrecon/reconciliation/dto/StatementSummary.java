package com.payrecon.reconciliation.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// One uploaded bank statement, summarised: how many lines, what total, when uploaded
public record StatementSummary(
        UUID statementId,
        long lines,
        BigDecimal totalAmount,
        Instant uploadedAt) { }
