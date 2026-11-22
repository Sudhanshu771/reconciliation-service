package com.payrecon.reconciliation.repository;

import com.payrecon.reconciliation.model.ReconciliationRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ReconciliationRunRepository extends JpaRepository<ReconciliationRun, UUID> {

    // NEW: all runs, newest first  →  select ... order by run_at desc
    List<ReconciliationRun> findAllByOrderByRunAtDesc();
}
