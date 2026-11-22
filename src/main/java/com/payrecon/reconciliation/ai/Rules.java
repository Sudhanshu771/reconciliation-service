package com.payrecon.reconciliation.ai;

import com.payrecon.reconciliation.model.ExpectedPayment;

import java.math.BigDecimal;

// The fallback when the AI is off or down: simple, predictable hints written as code.
final class Rules {

    private Rules() { }

    static String explain(ExplanationService.Facts f) {
        return switch (f.result().getStatus()) {
            case AMOUNT_MISMATCH -> amountMismatch(f);
            case MISSING_IN_BANK -> "The bank statement has no line for reference " + f.result().getReference()
                    + ". The payment may still be in transit (check the next statement), may have failed at the bank,"
                    + " or the bank may show a different reference. Check the payment status with the bank.";
            case MISSING_IN_OURS -> missingInOurs(f);
            case MATCHED -> "This line matched; nothing to explain.";
        };
    }

    private static String amountMismatch(ExplanationService.Facts f) {
        BigDecimal diff = f.result().getDifference();
        if (diff == null) return "The amounts differ. Compare both records manually.";
        if (diff.signum() < 0) {
            return "The bank received " + diff.abs() + " less than we sent. Common causes: a bank charge deducted"
                    + " from the transfer, or a partial payment. Check the statement for a matching fee entry"
                    + " and confirm the amount with the bank.";
        }
        return "The bank received " + diff + " more than we sent. Common causes: two payments combined into one"
                + " transfer, or a wrong amount entered. Check for another payment with the same reference.";
    }

    private static String missingInOurs(ExplanationService.Facts f) {
        if (f.candidates().isEmpty()) {
            return "The bank shows this credit but we have no payment with reference " + f.result().getReference()
                    + " and no similar reference or same amount. It may be a payment made outside the system,"
                    + " a bank fee or interest, or something to escalate.";
        }
        ExpectedPayment best = f.candidates().get(0);
        int distance = Similarity.referenceDistance(best.getReference(), f.result().getReference());
        String sameAmount = f.bank() != null && best.getAmount().compareTo(f.bank().getAmount()) == 0
                ? " and the same amount" : ", but a different amount (" + best.getAmount() + ")";
        return "No payment has reference " + f.result().getReference() + ". The closest is " + best.getReference()
                + " (" + distance + " character(s) different" + sameAmount + "). It may be a typo in the reference."
                + " Verify before linking them.";
    }
}
