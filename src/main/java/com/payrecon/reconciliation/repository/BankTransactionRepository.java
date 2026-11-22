package com.payrecon.reconciliation.repository;

import com.payrecon.reconciliation.model.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, UUID> {

    boolean existsByBankTxnId(String bankTxnId);

    // NEW: all lines from one uploaded statement
    List<BankTransaction> findByStatementId(UUID statementId);
}