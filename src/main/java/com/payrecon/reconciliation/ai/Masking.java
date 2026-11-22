package com.payrecon.reconciliation.ai;

// Hide most of an account number before it leaves our system:
//   "HDFC0001234-50100234567814"  →  "HDFC****7814"
final class Masking {

    private Masking() { }

    static String account(String account) {
        if (account == null || account.isBlank()) return "(none)";
        String a = account.strip();
        if (a.length() <= 8) return "****";
        return a.substring(0, 4) + "****" + a.substring(a.length() - 4);
    }
}
