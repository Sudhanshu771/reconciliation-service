package com.payrecon.reconciliation.dto;

import com.payrecon.reconciliation.model.AiExplanation;

import java.time.Instant;
import java.util.UUID;

public record ExplanationResponse(
        UUID resultId,
        String source,          // "AI" or "RULES"
        String model,
        String explanation,
        String requestedBy,
        Instant createdAt) {

    public static ExplanationResponse from(AiExplanation e) {
        return new ExplanationResponse(e.getResultId(), e.getSource().name(), e.getModel(),
                e.getExplanation(), e.getRequestedBy(), e.getCreatedAt());
    }
}
