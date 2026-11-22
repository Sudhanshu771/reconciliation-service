package com.payrecon.reconciliation.service;

import com.payrecon.reconciliation.dto.ReconciliationReport;
import com.payrecon.reconciliation.model.BankTransaction;
import com.payrecon.reconciliation.model.ExpectedPayment;
import com.payrecon.reconciliation.model.MatchStatus;
import com.payrecon.reconciliation.model.ReconciliationRun;
import com.payrecon.reconciliation.repository.BankTransactionRepository;
import com.payrecon.reconciliation.repository.ExpectedPaymentRepository;
import com.payrecon.reconciliation.repository.ReconciliationResultRepository;
import com.payrecon.reconciliation.repository.ReconciliationRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Unit tests for the matching rules. No database, no Spring: the repositories are Mockito "fakes".
class ReconciliationServiceTest {

    private static final UUID STATEMENT = UUID.randomUUID();
    private static final LocalDate STATEMENT_DAY = LocalDate.of(2026, 11, 22);
    private static final Instant SAME_DAY = Instant.parse("2026-11-22T06:00:00Z");   // 11:30 IST
    private static final Instant NEXT_DAY = Instant.parse("2026-11-23T06:00:00Z");

    private BankTransactionRepository bankRepo;
    private ExpectedPaymentRepository expectedRepo;
    private ReconciliationResultRepository resultRepo;
    private ReconciliationService service;

    @BeforeEach
    void setUp() {
        bankRepo = mock(BankTransactionRepository.class);
        expectedRepo = mock(ExpectedPaymentRepository.class);
        resultRepo = mock(ReconciliationResultRepository.class);
        ReconciliationRunRepository runRepo = mock(ReconciliationRunRepository.class);
        when(runRepo.save(any())).thenAnswer(call -> call.<ReconciliationRun>getArgument(0));
        when(resultRepo.findPaymentsClaimedByOtherStatements(any(), any())).thenReturn(Set.of());
        service = new ReconciliationService(bankRepo, expectedRepo, runRepo, resultRepo);
    }

    @Test
    void sameAmountIsPreferred_andEachPaymentIsUsedOnlyOnce() {
        ExpectedPayment a = payment("FAIL-1", "2600.00", SAME_DAY);
        ExpectedPayment b = payment("FAIL-1", "2600.00", SAME_DAY);
        bankLines(bank("UTR1", "FAIL-1", "2500.00"), bank("UTR2", "FAIL-1", "2600.00"));
        when(expectedRepo.findAll()).thenReturn(List.of(a, b));

        ReconciliationReport report = service.reconcile(STATEMENT);

        assertThat(statusOf(report, "UTR2")).isEqualTo(MatchStatus.MATCHED);
        assertThat(statusOf(report, "UTR1")).isEqualTo(MatchStatus.AMOUNT_MISMATCH);
        assertThat(report.missingInBank()).isZero();             // both payments were used, once each
    }

    @Test
    void paymentsMatchedByAnotherStatement_areNotReportedAgain() {
        ExpectedPayment alreadyMatched = payment("INV-1", "100.00", SAME_DAY);
        ExpectedPayment open = payment("INV-2", "200.00", SAME_DAY);
        bankLines(bank("UTR9", "INV-2", "200.00"));
        when(expectedRepo.findAll()).thenReturn(List.of(alreadyMatched, open));
        when(resultRepo.findPaymentsClaimedByOtherStatements(any(), any()))
                .thenReturn(Set.of(alreadyMatched.getPaymentId()));

        ReconciliationReport report = service.reconcile(STATEMENT);

        assertThat(report.matched()).isEqualTo(1);
        assertThat(report.missingInBank()).isZero();             // INV-1 is NOT "missing in bank" any more
    }

    @Test
    void paymentsMadeAfterTheStatementPeriod_areIgnored() {
        ExpectedPayment later = payment("INV-3", "300.00", NEXT_DAY);
        bankLines(bank("UTR5", "INV-4", "400.00"));
        when(expectedRepo.findAll()).thenReturn(List.of(later));

        ReconciliationReport report = service.reconcile(STATEMENT);

        assertThat(report.missingInBank()).isZero();             // tomorrow's payment can't be on today's statement
        assertThat(report.missingInOurs()).isEqualTo(1);
    }

    @Test
    void unmatchedPaymentInThePeriod_isMissingInBank() {
        ExpectedPayment notPaidYet = payment("INV-5", "500.00", SAME_DAY);
        bankLines(bank("UTR6", "INV-6", "600.00"));
        when(expectedRepo.findAll()).thenReturn(List.of(notPaidYet));

        ReconciliationReport report = service.reconcile(STATEMENT);

        assertThat(report.missingInBank()).isEqualTo(1);
        assertThat(report.missingInOurs()).isEqualTo(1);
    }

    // ---------- helpers ----------

    private void bankLines(BankTransaction... lines) {
        when(bankRepo.findByStatementId(STATEMENT)).thenReturn(List.of(lines));
    }

    private static ExpectedPayment payment(String reference, String amount, Instant createdAt) {
        return new ExpectedPayment(UUID.randomUUID(), new BigDecimal(amount), "INR", reference,
                "HDFC0001234-50100234567814", "ICIC0004321-002301556789", createdAt);
    }

    private static BankTransaction bank(String txnId, String reference, String amount) {
        return new BankTransaction(STATEMENT, txnId, reference, new BigDecimal(amount), "INR", STATEMENT_DAY);
    }

    private static MatchStatus statusOf(ReconciliationReport report, String bankTxnId) {
        return report.results().stream()
                .filter(line -> bankTxnId.equals(line.bankTxnId()))
                .findFirst().orElseThrow()
                .status();
    }
}
