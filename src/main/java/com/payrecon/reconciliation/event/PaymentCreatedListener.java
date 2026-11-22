package com.payrecon.reconciliation.event;

import com.payrecon.reconciliation.model.ExpectedPayment;
import com.payrecon.reconciliation.repository.ExpectedPaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentCreatedListener.class);
    private final ExpectedPaymentRepository repository;

    public PaymentCreatedListener(ExpectedPaymentRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "payment.created", groupId = "reconciliation")
    public void onPaymentCreated(PaymentCreatedEvent event) {

        // Kafka can deliver the same message twice → skip if we already have it
        if (repository.existsById(event.paymentId())) {
            log.info("DUPLICATE payment {} skipped", event.paymentId());
            return;
        }

        repository.save(new ExpectedPayment(
                event.paymentId(), event.amount(), event.currency(), event.reference(),
                event.payerAccount(), event.payeeAccount(), event.createdAt()));

        log.info("SAVED expected payment {} ref {} amount {}",
                event.paymentId(), event.reference(), event.amount());
    }
}