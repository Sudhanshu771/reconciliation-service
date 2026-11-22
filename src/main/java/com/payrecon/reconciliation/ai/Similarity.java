package com.payrecon.reconciliation.ai;

import java.util.Locale;

// How different are two references? (Levenshtein "edit distance")
//   "INV-9001" vs "INV-90018" → 1 (one character missing) → probably a typo
final class Similarity {

    private Similarity() { }

    static int referenceDistance(String a, String b) {
        if (a == null || b == null) return Integer.MAX_VALUE;
        String s = a.strip().toUpperCase(Locale.ROOT);
        String t = b.strip().toUpperCase(Locale.ROOT);

        int[] prev = new int[t.length() + 1];
        int[] curr = new int[t.length() + 1];
        for (int j = 0; j <= t.length(); j++) prev[j] = j;

        for (int i = 1; i <= s.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= t.length(); j++) {
                int cost = s.charAt(i - 1) == t.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] swap = prev; prev = curr; curr = swap;
        }
        return prev[t.length()];
    }
}
