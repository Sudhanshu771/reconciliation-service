package com.payrecon.reconciliation.repository;

import com.payrecon.reconciliation.model.MatchStatus;
import com.payrecon.reconciliation.model.ReconciliationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ReconciliationResultRepository extends JpaRepository<ReconciliationResult, UUID> {

    List<ReconciliationResult> findByRunId(UUID runId);

    // Payments that OTHER statements already linked to a bank line (matched or amount mismatch).
    // Only each statement's LATEST run counts — re-running a statement replaces its earlier answer.
    @Query("""
            select distinct r.expectedPaymentId
            from ReconciliationResult r, ReconciliationRun run
            where r.runId = run.id
              and run.statementId <> :statementId
              and r.expectedPaymentId is not null
              and r.status in :statuses
              and run.runAt = (select max(latest.runAt) from ReconciliationRun latest
                               where latest.statementId = run.statementId)
            """)
    Set<UUID> findPaymentsClaimedByOtherStatements(@Param("statementId") UUID statementId,
                                                   @Param("statuses") Collection<MatchStatus> statuses);
}
