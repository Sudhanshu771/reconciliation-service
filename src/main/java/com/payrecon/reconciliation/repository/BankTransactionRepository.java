package com.payrecon.reconciliation.repository;

import com.payrecon.reconciliation.dto.StatementSummary;
import com.payrecon.reconciliation.model.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, UUID> {

    boolean existsByBankTxnId(String bankTxnId);

    Optional<BankTransaction> findByBankTxnId(String bankTxnId);

    // All lines from one uploaded statement
    List<BankTransaction> findByStatementId(UUID statementId);

    // NEW: one summary row per uploaded statement (a GROUP BY written in JPQL).
    // "select new ...StatementSummary(...)" builds our record directly from each result row.
    @Query("""
            select new com.payrecon.reconciliation.dto.StatementSummary(
                       b.statementId, count(b), sum(b.amount), min(b.uploadedAt))
            from BankTransaction b
            group by b.statementId
            order by min(b.uploadedAt) desc
            """)
    List<StatementSummary> summarizeStatements();
}
