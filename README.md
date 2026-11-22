# reconciliation-service

Checks that **our payments** and the **bank's statement** agree, and uses **AI** to explain the lines that don't.
Built with **Spring Boot 4**, **PostgreSQL**, **Kafka** and **Spring AI (OpenAI)**.

**Platform:** [payment-service](https://github.com/Sudhanshu771/payment-service) · reconciliation-service · [payrecon-dashboard](https://github.com/Sudhanshu771/payrecon-dashboard)

```
payment-service ──Kafka "payment.created"──▶ expected_payments ┐
                                                                ├─▶ MATCH ──▶ runs + results ──▶ ✨ AI explanation
bank statement CSV ──POST /statements──────▶ bank_transactions ┘
```

## Features

- **Kafka consumer**: every `payment.created` event is stored as an *expected payment* (consumer group `reconciliation`)
  - duplicates are skipped (the payment id is the primary key), so at-least-once delivery is safe
  - **retries with backoff** (3 tries, 2 s apart), then a **dead-letter topic** `payment.created-dlt` with the error in its headers
  - bad data (`IllegalArgumentException`) skips retries; invalid JSON can't block the topic (`ErrorHandlingDeserializer`)
- **Bank statement upload**: multipart CSV → `bank_transactions`; bad lines are reported, not fatal; re-uploads are de-duplicated by the bank's transaction id
- **Reconciliation**: each bank line is matched to a payment and gets one of
  - ✅ `MATCHED` · ⚠️ `AMOUNT_MISMATCH` · ❌ `MISSING_IN_BANK` · ❌ `MISSING_IN_OURS`
- **AI explanations (Spring AI + OpenAI)**: "Explain" a broken line in plain English, with banking-safe rules (see below)
- **Security**: only `AUDITOR` and `ADMIN` users, with a JWT issued by payment-service

## Matching rules

1. Group our payments by **reference**
2. For each bank line, pick the best candidate with the same reference: **same amount first**, then the **closest date** (IST)
3. **One-to-one**: a payment can be used by only one bank line
4. Same amount → `MATCHED`; different amount → `AMOUNT_MISMATCH` (difference = bank − ours); no candidate → `MISSING_IN_OURS`
5. Payments left over → `MISSING_IN_BANK`
6. **No double counting**: payments already matched by **another statement's latest run**, and payments made **after the statement's last date**, are left out
7. Each run is saved (`reconciliation_runs` + `reconciliation_results`) in **one transaction**: an append-only audit history

## AI explanations (step 7)

`POST /api/v1/reconciliations/results/{resultId}/explanation` → a short explanation and what to check next.

| Rule | How |
|---|---|
| **Masking** | account numbers are masked (`HDFC****7814`) before anything is sent |
| **Facts only** | the prompt has the line's facts plus similar candidates (Levenshtein distance on references); the model is told not to invent data |
| **Suggest, don't decide** | nothing is changed automatically; the UI says "review before acting" |
| **Audit** | every answer is stored in `ai_explanations` (who asked, AI or rules, which model) |
| **Fallback** | no `OPENAI_API_KEY`, or the AI call fails → a rule-based hint, so the feature always works |
| **Cost control** | the saved answer is reused unless `?refresh=true` |

## API

All endpoints need `Authorization: Bearer <token>` (role `AUDITOR` or `ADMIN`).

| Method | URL | What |
|---|---|---|
| `POST` | `/api/v1/statements` (multipart, field `file`) | upload a bank CSV → `{statementId, saved, skippedDuplicates, errors}` |
| `GET` | `/api/v1/statements` | uploaded statements (lines, total, upload time) |
| `POST` | `/api/v1/reconciliations?statementId=…` | run the matching → full report |
| `GET` | `/api/v1/reconciliations` | all runs (summaries), newest first |
| `GET` | `/api/v1/reconciliations/{runId}` | one run with every result line |
| `POST` | `/api/v1/reconciliations/results/{resultId}/explanation` | explain a line (AI or rules) |
| `GET` | `/api/v1/reconciliations/results/{resultId}/explanation` | the saved explanation, or 404 |

CSV format:
```csv
bank_txn_id,reference,amount,currency,value_date
UTR1001,INV-90018,2600.00,INR,2026-11-22
```

Errors use `ProblemDetail` (RFC 9457): 400 bad input · 401 no/invalid token · 403 wrong role · 404 not found · 503 database down.

## Tech stack

| Area | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.2 (Web MVC, Data JPA, Validation, Security / OAuth2 Resource Server) |
| Database | PostgreSQL 18 |
| Messaging | Apache Kafka 4, Spring for Apache Kafka (DLT, `ErrorHandlingDeserializer`) |
| AI | Spring AI 2.1 with OpenAI (`gpt-4o-mini` by default) |
| Tests | JUnit 5, Mockito, AssertJ |

## Running locally

**Prerequisites:** Java 17+, PostgreSQL with a database named `reconciliation`, Kafka on `localhost:9092`, and payment-service running (it issues the login tokens).

```powershell
.\mvnw.cmd spring-boot:run
```
The API runs at `http://localhost:8081`.

Run the unit tests for the matching rules:
```powershell
.\mvnw.cmd test -Dtest=ReconciliationServiceTest
```

### Configuration (environment variables)
| Variable | Purpose | Default |
|---|---|---|
| `DB_PASSWORD` | PostgreSQL password | `postgres` |
| `JWT_SECRET` | checks tokens; **must match payment-service** | a local-dev-only value |
| `OPENAI_API_KEY` | enables AI explanations (never commit it) | not set → rule-based hints |
| `OPENAI_MODEL` | OpenAI model | `gpt-4o-mini` |

## Known limitations / next steps

- References are compared exactly when matching (fuzzy matching is only used for AI hints)
- A human "accept this suggestion / link these records" workflow isn't built yet
- Docker Compose and deployment
