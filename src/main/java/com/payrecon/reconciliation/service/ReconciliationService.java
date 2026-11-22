package com.payrecon.reconciliation.service;

import com.payrecon.reconciliation.dto.ReconciliationReport;
import com.payrecon.reconciliation.dto.RunSummary;
import com.payrecon.reconciliation.model.BankTransaction;
import com.payrecon.reconciliation.model.ExpectedPayment;
import com.payrecon.reconciliation.model.MatchStatus;
import com.payrecon.reconciliation.model.ReconciliationResult;
import com.payrecon.reconciliation.model.ReconciliationRun;
import com.payrecon.reconciliation.repository.BankTransactionRepository;
import com.payrecon.reconciliation.repository.ExpectedPaymentRepository;
import com.payrecon.reconciliation.repository.ReconciliationResultRepository;
import com.payrecon.reconciliation.repository.ReconciliationRunRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReconciliationService {

    // Bank statements in India use Indian dates, so we compare dates in IST
    private static final ZoneId BANK_ZONE = ZoneId.of("Asia/Kolkata");

    private final BankTransactionRepository bankRepo;
    private final ExpectedPaymentRepository expectedRepo;
    private final ReconciliationRunRepository runRepo;
    private final ReconciliationResultRepository resultRepo;

    public ReconciliationService(BankTransactionRepository bankRepo,
                                 ExpectedPaymentRepository expectedRepo,
                                 ReconciliationRunRepository runRepo,
                                 ReconciliationResultRepository resultRepo) {
        this.bankRepo = bankRepo;
        this.expectedRepo = expectedRepo;
        this.runRepo = runRepo;
        this.resultRepo = resultRepo;
    }

    @Transactional      // all results are saved together, or none at all
    public ReconciliationReport reconcile(UUID statementId) {

        // 1. Load the bank side (this statement only)
        List<BankTransaction> bankLines = bankRepo.findByStatementId(statementId);
        if (bankLines.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No bank statement found with id " + statementId);
        }

        // 2. Load our side and group it by reference:
        //    "INV-90018" -> [payment1, payment2, payment3, payment4]
        //    (simple version: ALL expected payments; a real system would limit it to a date range)
        Map<String, List<ExpectedPayment>> unmatchedByRef = new HashMap<>();
        for (ExpectedPayment p : expectedRepo.findAll()) {
            unmatchedByRef.computeIfAbsent(p.getReference(), ref -> new ArrayList<>()).add(p);
        }

        ReconciliationRun run = runRepo.save(new ReconciliationRun(statementId));
        List<ReconciliationResult> results = new ArrayList<>();

        // 3. Walk through every bank line and look for its partner on our side
        for (BankTransaction bank : bankLines) {
            List<ExpectedPayment> candidates =
                    unmatchedByRef.getOrDefault(bank.getReference(), new ArrayList<>());

            if (candidates.isEmpty()) {
                // The bank has it, we don't → MISSING_IN_OURS
                results.add(new ReconciliationResult(run.getId(), MatchStatus.MISSING_IN_OURS,
                        bank.getReference(), null, bank.getBankTxnId(), null, bank.getAmount()));
                continue;
            }

            ExpectedPayment best = pickBest(candidates, bank);
            candidates.remove(best);    // ONE-TO-ONE: this payment can't be matched again

            MatchStatus status = best.getAmount().compareTo(bank.getAmount()) == 0
                    ? MatchStatus.MATCHED
                    : MatchStatus.AMOUNT_MISMATCH;

            results.add(new ReconciliationResult(run.getId(), status, bank.getReference(),
                    best.getPaymentId(), bank.getBankTxnId(), best.getAmount(), bank.getAmount()));
        }

        // 4. Whatever is still unmatched on our side: we sent it, the bank never showed it
        for (List<ExpectedPayment> leftovers : unmatchedByRef.values()) {
            for (ExpectedPayment p : leftovers) {
                results.add(new ReconciliationResult(run.getId(), MatchStatus.MISSING_IN_BANK,
                        p.getReference(), p.getPaymentId(), null, p.getAmount(), null));
            }
        }

        // 5. Count the totals and save everything
        results.forEach(r -> run.count(r.getStatus()));
        resultRepo.saveAll(results);

        return ReconciliationReport.from(run, results);
    }

    // NEW (piece 5): list all runs, newest first — summaries only
    @Transactional(readOnly = true)
    public List<RunSummary> listRuns() {
        return runRepo.findAllByOrderByRunAtDesc().stream()
                .map(RunSummary::from)
                .toList();
    }

    // NEW (piece 5): one run with all its result lines
    @Transactional(readOnly = true)
    public ReconciliationReport getRun(UUID runId) {
        ReconciliationRun run = runRepo.findById(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Reconciliation run not found: " + runId));
        return ReconciliationReport.from(run, resultRepo.findByRunId(runId));
    }

    // Several of our payments have the same reference: which one belongs to this bank line?
    //  1st choice: same amount   (so a correct match isn't wasted on a wrong-amount payment)
    //  2nd choice: closest date  (tie-breaker)
    private ExpectedPayment pickBest(List<ExpectedPayment> candidates, BankTransaction bank) {
        Comparator<ExpectedPayment> sameAmountFirst =
                Comparator.comparing(p -> p.getAmount().compareTo(bank.getAmount()) == 0 ? 0 : 1);
        Comparator<ExpectedPayment> closestDate =
                Comparator.comparingLong(p -> daysApart(p, bank.getValueDate()));

        return candidates.stream()
                .min(sameAmountFirst.thenComparing(closestDate))
                .orElseThrow();
    }

    private long daysApart(ExpectedPayment p, LocalDate bankDate) {
        LocalDate paidOn = p.getCreatedAt().atZone(BANK_ZONE).toLocalDate();
        return Math.abs(ChronoUnit.DAYS.between(paidOn, bankDate));
    }
}
