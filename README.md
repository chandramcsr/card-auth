# Card Authorization

A Java service that authorizes credit card transactions against an account's available credit.

## Tech stack

- Java 17+
- Maven
- JUnit 5

## How it works

`AuthorizationService.authorize(request)`:

1. Returns the stored result if the `requestId` was already processed (idempotent replay).
2. Looks up the card; declines with `CARD_NOT_FOUND` if missing.
3. Runs the rule pipeline in order; the first rule that objects sets the decline reason.
4. On approval, places a hold that reduces the card's available credit and returns an `authorizationId`.

`AuthorizationService.reverse(authorizationId)` releases the hold and restores available credit. Reversing twice has no further effect.

### Default rules (in order)

| Rule | Decline reason |
|---|---|
| `AmountPositiveRule` | `INVALID_AMOUNT` |
| `CardStatusRule` | `CARD_NOT_ACTIVE` |
| `CardExpiryRule` (valid through the end of the expiration month) | `CARD_EXPIRED` |
| `CurrencyMatchRule` | `CURRENCY_MISMATCH` |
| `SufficientCreditRule` | `INSUFFICIENT_CREDIT` |

### Adding a rule

Implement `AuthorizationRule` and add it to the list passed to the service:

```java
AuthorizationRule maxPerTransaction = ctx ->
        ctx.request().amount().compareTo(Money.usd(500_00)) > 0
                ? Optional.of(DeclineReason.INSUFFICIENT_CREDIT)
                : Optional.empty();
```

Persistence sits behind `CardAccountRepository` and `AuthorizationRepository`; in-memory implementations are included.

## Usage

```java
var cards = new InMemoryCardAccountRepository();
cards.save(CardAccount.open("card-1", YearMonth.of(2030, 12), Money.usd(100_00)));

var service = new AuthorizationService(
        cards, new InMemoryAuthorizationRepository(),
        AuthorizationRules.defaults(), Clock.systemUTC());

AuthorizationResult result = service.authorize(
        new AuthorizationRequest("req-1", "card-1", Money.usd(60_00), "Coffee Co"));
```

Amounts are `Money` values in minor units (cents) with a currency.

## Build and test

```
mvn test
mvn compile exec:java -Dexec.mainClass=cardauth.Main
```
