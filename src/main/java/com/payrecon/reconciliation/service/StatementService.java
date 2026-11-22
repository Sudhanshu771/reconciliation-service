package com.payrecon.reconciliation.service;

import com.payrecon.reconciliation.dto.StatementUploadResult;
import com.payrecon.reconciliation.model.BankTransaction;
import com.payrecon.reconciliation.repository.BankTransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class StatementService {

    private final BankTransactionRepository repository;

    public StatementService(BankTransactionRepository repository) {
        this.repository = repository;
    }

    public StatementUploadResult upload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        UUID statementId = UUID.randomUUID();      // one id for this whole upload
        int saved = 0;
        int duplicates = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNo = 0;

            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (lineNo == 1 || line.isBlank()) {
                    continue;                      // skip the header row and empty lines
                }

                String[] cols = line.split(",", -1);
                if (cols.length != 5) {
                    errors.add("Line " + lineNo + ": expected 5 columns, found " + cols.length);
                    continue;
                }

                try {
                    String bankTxnId = cols[0].trim();
                    String reference = cols[1].trim();
                    BigDecimal amount = new BigDecimal(cols[2].trim());
                    String currency = cols[3].trim();
                    LocalDate valueDate = LocalDate.parse(cols[4].trim());

                    if (repository.existsByBankTxnId(bankTxnId)) {
                        duplicates++;              // already uploaded before → skip
                        continue;
                    }

                    repository.save(new BankTransaction(
                            statementId, bankTxnId, reference, amount, currency, valueDate));
                    saved++;

                } catch (NumberFormatException e) {
                    errors.add("Line " + lineNo + ": invalid amount '" + cols[2].trim() + "'");
                } catch (DateTimeParseException e) {
                    errors.add("Line " + lineNo + ": invalid date '" + cols[4].trim() + "' (use yyyy-MM-dd)");
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read file", e);
        }

        return new StatementUploadResult(statementId, saved, duplicates, errors);
    }
}