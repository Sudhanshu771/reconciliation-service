package com.payrecon.reconciliation.model;

// The 4 possible outcomes when we compare our payments with the bank statement
public enum MatchStatus {
    MATCHED,            // on both sides, same amount
    AMOUNT_MISMATCH,    // on both sides, different amount
    MISSING_IN_BANK,    // we sent it, the bank doesn't show it
    MISSING_IN_OURS     // the bank shows it, we never sent it
}
