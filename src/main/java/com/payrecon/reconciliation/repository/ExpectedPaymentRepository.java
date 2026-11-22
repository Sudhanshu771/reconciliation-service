package com.payrecon.reconciliation.repository;

import com.payrecon.reconciliation.model.ExpectedPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ExpectedPaymentRepository extends JpaRepository<ExpectedPayment, UUID> { }