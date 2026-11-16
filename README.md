# Card Authorization

A Spring Boot service that authorizes credit card transactions against an account's available credit.

## Tech stack

- Java 17
- Spring Boot 3.2 (Web, Validation)
- Gradle 8 (wrapper included)
- JUnit 5 and MockMvc

## Run

```
./gradlew bootRun
```

The service starts on port 8080 with one seeded card, `demo-card`, with a $1,000 limit. Set `cardauth.seed-demo-card=false` to start empty. Run the tests with `./gradlew test`.

## API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/cards` | Create a card. `201`, or `409` if the id exists. |
| `GET` | `/api/cards/{cardId}` | Card status, limit and available credit. |
| `POST` | `/api/authorizations` | Authorize a transaction. Returns `200` for both approvals and declines. |
| `POST` | `/api/authorizations/{authorizationId}/reversal` | Release the hold. `204`, or `404` if unknown. Reversing twice has no further effect. |

Amounts are integers in minor units (cents) with an ISO currency code.

```
curl -X POST localhost:8080/api/authorizations -H 'Content-Type: application/json' -d '{
  "requestId": "req-1",
  "cardId": "demo-card",
  "amountCents": 6000,
  "currency": "USD",
  "merchantName": "Coffee Co"
}'
```

```
{"requestId":"req-1","decision":"APPROVED","authorizationId":"<uuid>","declineReason":null}
```

Resubmitting the same `requestId` returns the original result and does not hold funds twice. Malformed requests return `400`.

## How authorization works

`AuthorizationService.authorize`:

1. Returns the stored result if the `requestId` was already processed.
2. Looks up the card; declines with `CARD_NOT_FOUND` if missing.
3. Runs the rule pipeline in order; the first rule that objects sets the decline reason.
4. On approval, holds the amount against available credit and returns an `authorizationId`.

### Default rules (in order)

| Rule | Decline reason |
|---|---|
| `AmountPositiveRule` | `INVALID_AMOUNT` |
| `CardStatusRule` | `CARD_NOT_ACTIVE` |
| `CardExpiryRule` (valid through the end of the expiration month) | `CARD_EXPIRED` |
| `CurrencyMatchRule` | `CURRENCY_MISMATCH` |
| `SufficientCreditRule` | `INSUFFICIENT_CREDIT` |

### Adding a rule

Implement `AuthorizationRule` and add it to the list in `AuthorizationRules.defaults()`:

```java
AuthorizationRule maxPerTransaction = ctx ->
        ctx.request().amount().compareTo(Money.usd(500_00)) > 0
                ? Optional.of(DeclineReason.INSUFFICIENT_CREDIT)
                : Optional.empty();
```

## Structure

| Package | Contents |
|---|---|
| `cardauth.model` | `Money`, `CardAccount`, requests, results, holds |
| `cardauth.rules` | Authorization rules and the context they evaluate |
| `cardauth.repository` | Repository interfaces and in-memory implementations |
| `cardauth.service` | `AuthorizationService` |
| `cardauth.api` | REST controllers, DTOs, error handling, Spring wiring |
