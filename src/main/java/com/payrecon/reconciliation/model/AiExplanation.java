package com.payrecon.reconciliation.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

// AUDIT TRAIL: every explanation we showed to a user is stored — who asked, for which line,
// what was answered, and whether it came from the AI model or from the simple rules fallback.
@Entity
@Table(name = "ai_explanations")
public class AiExplanation {

    public enum Source { AI, RULES }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID resultId;               // which reconciliation_results line

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Source source;

    private String model;                // e.g. "gpt-4o-mini" (null for RULES)

    @Column(nullable = false, length = 4000)
    private String explanation;

    @Column(nullable = false)
    private String requestedBy;          // username from the JWT

    @Column(nullable = false)
    private Instant createdAt;

    protected AiExplanation() { }

    public AiExplanation(UUID resultId, Source source, String model, String explanation, String requestedBy) {
        this.resultId = resultId;
        this.source = source;
        this.model = model;
        this.explanation = explanation;
        this.requestedBy = requestedBy;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getResultId() { return resultId; }
    public Source getSource() { return source; }
    public String getModel() { return model; }
    public String getExplanation() { return explanation; }
    public String getRequestedBy() { return requestedBy; }
    public Instant getCreatedAt() { return createdAt; }
}
