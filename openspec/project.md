# Project Context

## Purpose
Card Authorization Service decides whether a credit card transaction may proceed. It checks a
request against a card account through an ordered pipeline of rules. When a request is approved,
the service places a hold on the account's available credit. A reversal releases the hold. The
service runs as a Spring Boot REST API.

## Tech Stack
- Java 21 (Gradle toolchain)
- Spring Boot 3.2 (`spring-boot-starter-web`, `spring-boot-starter-validation`)
- Gradle 8 (wrapper included)
- JUnit 5, AssertJ, MockMvc (`spring-boot-starter-test`)
- Postman collection in `postman/collections/Card Authorization Service`

## Project Conventions

### Code Style
- Use Java records for immutable values and DTOs (`Money`, `Hold`, `AuthorizationRequest`, API requests and responses).
- Keep the domain packages (`model`, `rules`, `repository`, `service`) free of framework code. Spring code lives only in `api`.
- Constructors validate their inputs and fail fast with `IllegalArgumentException` or `NullPointerException`.
- Javadoc explains *why* code is written the way it is, not what it does.

### Architecture Patterns
- Base package: `com.chandra.cardauth`.
  - `model`: `Money`, `CardAccount`, `CardStatus`, `AuthorizationRequest`, `AuthorizationResult`, `DeclineReason`, `Hold`
  - `rules`: `AuthorizationRule` (functional interface), `AuthorizationContext`, and the default rules
  - `repository`: repository interfaces with in-memory `ConcurrentHashMap` implementations
  - `service`: `AuthorizationService`, which orchestrates idempotency, the rules, holds and reversals
  - `api`: REST controllers, DTOs, `ApiExceptionHandler` (RFC 7807 `ProblemDetail`), Spring wiring (`AuthorizationConfig`), and the demo seed
- The rule pipeline uses a chain of responsibility. The first rule that objects decides the decline reason.
- `Clock` and the authorization ID generator are injected, so time and IDs are deterministic in tests.
- `authorize` and `reverse` are `synchronized` on the service. This is a deliberate single-lock simplification.

### Testing Strategy
- Unit tests cover the model (`MoneyTest`, `CardAccountTest`), each rule (`RulesTest`), and the service (`AuthorizationServiceTest`, which uses a fixed `Clock`).
- An API test (`CardAuthorizationApiTest`) runs the full Spring context through MockMvc.
- Every scenario in `openspec/specs` should map to at least one test.
- Run the tests with `./gradlew test`.

### Git Workflow
- Work on the `main` branch with short imperative commit messages (for example, "Expose authorization service as a Spring Boot REST API").

## Domain Context
- **Money** is a whole number of minor units (for example, cents) plus an ISO 4217 currency. The service never uses floating point. Arithmetic overflow throws an error, and mixing currencies throws an error.
- **Credit limit** is fixed when a card is opened. **Available credit** is the credit limit minus the active holds.
- **Hold**: credit reserved by an approved authorization. A hold is either active or released. Capture and settlement are not implemented yet.
- **Idempotency**: the `requestId` is the idempotency key. When a `requestId` is replayed, the service returns the stored original result, whether it was an approval or a decline.
- **Decline is not an error**: declines are normal business results and return HTTP 200.

## Important Constraints
- All state is in memory and is lost on restart. There is no persistence layer yet.
- The service processes one request at a time because of the single service-wide lock.
- There is no FX conversion. A transaction must be in the card's currency.
- There is no authentication or authorization on the API.

## External Dependencies
- None. The service is self-contained.
