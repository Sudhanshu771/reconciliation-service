package com.payrecon.reconciliation.ai;

import com.payrecon.reconciliation.dto.ExplanationResponse;
import com.payrecon.reconciliation.model.AiExplanation;
import com.payrecon.reconciliation.model.BankTransaction;
import com.payrecon.reconciliation.model.ExpectedPayment;
import com.payrecon.reconciliation.model.MatchStatus;
import com.payrecon.reconciliation.model.ReconciliationResult;
import com.payrecon.reconciliation.repository.AiExplanationRepository;
import com.payrecon.reconciliation.repository.BankTransactionRepository;
import com.payrecon.reconciliation.repository.ExpectedPaymentRepository;
import com.payrecon.reconciliation.repository.ReconciliationResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// STEP 7: "Explain this line" for the reconciliation report.
//
// Banking-safe rules built in:
//   1. MASKING   – account numbers are masked before anything is sent to the AI provider
//   2. SUGGEST   – the AI only explains / suggests; it never changes data. A human decides.
//   3. AUDIT     – every answer is saved (who asked, which line, AI or rules, which model)
//   4. FALLBACK  – no API key, or the AI call fails → a simple rule-based explanation instead
@Service
public class ExplanationService {

    private static final Logger log = LoggerFactory.getLogger(ExplanationService.class);
    private static final String KEY_NOT_SET = "not-set";

    private static final String SYSTEM_PROMPT = """
            You help a bank operations team reconcile payments against a bank statement (India, INR).
            You get the facts of ONE reconciliation line that did not match cleanly.
            Answer in plain English, 2 to 4 short sentences:
              - the most likely reason(s) it did not match,
              - what exactly a human should check next,
              - if candidate records are listed, say which one (if any) looks like the real match and why.
            Use ONLY the facts given. Never invent transactions, amounts or dates. If unsure, say so.
            You only SUGGEST; a human makes every decision.
            """;

    private final ChatClient chatClient;
    private final boolean aiEnabled;
    private final String modelName;
    private final ReconciliationResultRepository resultRepo;
    private final BankTransactionRepository bankRepo;
    private final ExpectedPaymentRepository expectedRepo;
    private final AiExplanationRepository explanationRepo;

    public ExplanationService(ChatClient.Builder chatClientBuilder,
                              @Value("${spring.ai.openai.api-key}") String apiKey,
                              @Value("${spring.ai.openai.chat.model}") String modelName,
                              ReconciliationResultRepository resultRepo,
                              BankTransactionRepository bankRepo,
                              ExpectedPaymentRepository expectedRepo,
                              AiExplanationRepository explanationRepo) {
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
        this.aiEnabled = apiKey != null && !apiKey.isBlank() && !KEY_NOT_SET.equals(apiKey);
        this.modelName = modelName;
        this.resultRepo = resultRepo;
        this.bankRepo = bankRepo;
        this.expectedRepo = expectedRepo;
        this.explanationRepo = explanationRepo;
        log.info("AI explanations: {}", aiEnabled ? "ON (model " + modelName + ")" : "OFF (no OPENAI_API_KEY) → rules only");
    }

    // The latest saved explanation for a line, if any
    public Optional<ExplanationResponse> latest(UUID resultId) {
        return explanationRepo.findFirstByResultIdOrderByCreatedAtDesc(resultId).map(ExplanationResponse::from);
    }

    // Explain a line. Reuses the saved answer unless refresh=true (AI calls cost money).
    public ExplanationResponse explain(UUID resultId, String username, boolean refresh) {
        ReconciliationResult result = resultRepo.findById(resultId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result line not found: " + resultId));
        if (result.getStatus() == MatchStatus.MATCHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Matched lines don't need an explanation");
        }
        if (!refresh) {
            Optional<ExplanationResponse> saved = latest(resultId);
            // Reuse a saved answer — but if the AI is now ON, don't keep serving an old rules-only hint
            if (saved.isPresent() && (!aiEnabled || "AI".equals(saved.get().source()))) {
                return saved.get();
            }
        }

        Facts facts = collectFacts(result);
        AiExplanation answer;
        if (aiEnabled) {
            try {
                String text = chatClient.prompt().user(facts.toPrompt()).call().content();
                answer = new AiExplanation(resultId, AiExplanation.Source.AI, modelName, trim(text), username);
            } catch (RuntimeException e) {
                log.warn("AI call failed for result {}, using rules instead: {}", resultId, e.getMessage());
                answer = new AiExplanation(resultId, AiExplanation.Source.RULES, null,
                        Rules.explain(facts) + " (The AI service was unavailable, so this is a rule-based hint.)",
                        username);
            }
        } else {
            answer = new AiExplanation(resultId, AiExplanation.Source.RULES, null, Rules.explain(facts), username);
        }
        return ExplanationResponse.from(explanationRepo.save(answer));
    }

    // ---------- Gather the facts (with MASKED accounts) ----------

    private Facts collectFacts(ReconciliationResult r) {
        BankTransaction bank = r.getBankTxnId() == null ? null : bankRepo.findByBankTxnId(r.getBankTxnId()).orElse(null);
        ExpectedPayment ours = r.getExpectedPaymentId() == null ? null : expectedRepo.findById(r.getExpectedPaymentId()).orElse(null);

        // For "missing in ours": which of OUR payments look similar? (typo in reference, same amount…)
        List<ExpectedPayment> candidates = List.of();
        if (r.getStatus() == MatchStatus.MISSING_IN_OURS && bank != null) {
            candidates = expectedRepo.findAll().stream()
                    .filter(p -> Similarity.referenceDistance(p.getReference(), bank.getReference()) <= 3
                            || p.getAmount().compareTo(bank.getAmount()) == 0)
                    .sorted(Comparator.comparingInt(p -> Similarity.referenceDistance(p.getReference(), bank.getReference())))
                    .limit(5)
                    .toList();
        }
        return new Facts(r, bank, ours, candidates);
    }

    private static String trim(String text) {
        if (text == null || text.isBlank()) return "The AI returned no answer.";
        String t = text.strip();
        return t.length() > 3900 ? t.substring(0, 3900) + "…" : t;
    }

    // ---------- The facts we send (nothing else leaves the system) ----------

    record Facts(ReconciliationResult result, BankTransaction bank, ExpectedPayment ours,
                 List<ExpectedPayment> candidates) {

        String toPrompt() {
            StringBuilder sb = new StringBuilder();
            sb.append("Status: ").append(result.getStatus()).append('\n');
            sb.append("Reference: ").append(result.getReference()).append('\n');
            if (ours != null) {
                sb.append("Our payment: amount ").append(ours.getAmount()).append(' ').append(ours.getCurrency())
                  .append(", created ").append(ours.getCreatedAt())
                  .append(", payer ").append(Masking.account(ours.getPayerAccount()))
                  .append(", payee ").append(Masking.account(ours.getPayeeAccount())).append('\n');
            } else {
                sb.append("Our payment: none found with this reference\n");
            }
            if (bank != null) {
                sb.append("Bank line: id ").append(bank.getBankTxnId())
                  .append(", amount ").append(bank.getAmount()).append(' ').append(bank.getCurrency())
                  .append(", value date ").append(bank.getValueDate()).append('\n');
            } else {
                sb.append("Bank line: none found on the statement\n");
            }
            BigDecimal diff = result.getDifference();
            if (diff != null) {
                sb.append("Difference (bank minus ours): ").append(diff).append('\n');
            }
            if (!candidates.isEmpty()) {
                sb.append("Candidate payments of ours with a similar reference or the same amount:\n");
                for (ExpectedPayment c : candidates) {
                    sb.append("  - reference ").append(c.getReference())
                      .append(", amount ").append(c.getAmount())
                      .append(", created ").append(c.getCreatedAt()).append('\n');
                }
            }
            return sb.toString();
        }
    }
}
