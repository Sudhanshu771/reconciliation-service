package com.payrecon.reconciliation.repository;

import com.payrecon.reconciliation.model.AiExplanation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiExplanationRepository extends JpaRepository<AiExplanation, UUID> {

    // The most recent explanation for a line (so we don't pay for the same AI call twice)
    Optional<AiExplanation> findFirstByResultIdOrderByCreatedAtDesc(UUID resultId);
}
