package com.payrecon.reconciliation.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// Turns every error into a clean ProblemDetail JSON (same format as payment-service).
//
// NEW compared with payment-service: we EXTEND ResponseEntityExceptionHandler.
// That Spring class already handles ~20 standard errors with the right status code, e.g.
//   405 wrong method, 415 wrong body type, 400 missing param / missing file / bad UUID,
//   404 unknown URL, and our own ResponseStatusException (404 "run not found", 400 "file is empty").
// So we only write handlers for what Spring does NOT know about.
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Database down / not answering → 503: temporary, the client should retry later
    @ExceptionHandler({CannotCreateTransactionException.class, DataAccessResourceFailureException.class})
    public ProblemDetail handleDatabaseDown(Exception ex) {
        log.error("Database unavailable", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Reconciliation is temporarily unavailable. Please try again shortly.");
        pd.setTitle("Service unavailable");
        return pd;
    }

    // Anything unexpected: log the details, never show them to the client → 500
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again later.");
        pd.setTitle("Internal error");
        return pd;
    }
}
