package com.payrecon.reconciliation.ai;

import com.payrecon.reconciliation.dto.ExplanationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reconciliations/results/{resultId}/explanation")
public class ExplanationController {

    private final ExplanationService explanationService;

    public ExplanationController(ExplanationService explanationService) {
        this.explanationService = explanationService;
    }

    // POST .../explanation          → explain (reuses the saved answer)
    // POST .../explanation?refresh=true → ask again
    // @AuthenticationPrincipal Jwt = the logged-in user's token; its "subject" is the username (for the audit)
    @PostMapping
    public ExplanationResponse explain(@PathVariable UUID resultId,
                                       @RequestParam(defaultValue = "false") boolean refresh,
                                       @AuthenticationPrincipal Jwt jwt) {
        return explanationService.explain(resultId, jwt.getSubject(), refresh);
    }

    // GET .../explanation → the saved explanation, or 404 if nobody asked yet
    @GetMapping
    public ResponseEntity<ExplanationResponse> latest(@PathVariable UUID resultId) {
        return ResponseEntity.of(explanationService.latest(resultId));
    }
}
