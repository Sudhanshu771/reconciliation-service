# reconciliation-service

Part of the **Payment Processing & Reconciliation Platform**
→ [payment-service](https://github.com/Sudhanshu771/payment-service) · reconciliation-service

## What it does
- Consumes `payment.created` events from Kafka and stores them as **expected payments**
- *(in progress)* Upload a bank statement CSV and match it against expected payments
- *(planned)* Results: ✅ matched · ⚠️ amount mismatch · ❌ missing

## Tech
Java 17 · Spring Boot 4 · PostgreSQL · Apache Kafka

## Run locally
1. PostgreSQL with a database named `reconciliation`
2. Kafka on `localhost:9092`
3. `./mvnw spring-boot:run` → http://localhost:8081
