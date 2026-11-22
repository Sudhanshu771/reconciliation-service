package com.payrecon.reconciliation.dto;

import java.util.List;
import java.util.UUID;

public record StatementUploadResult(
        UUID statementId,
        int saved,
        int skippedDuplicates,
        List<String> errors) { }