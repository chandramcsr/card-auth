# transaction-authorization Specification

## Purpose
This spec covers how a transaction is authorized. The service evaluates the request against the
card account through an ordered rule pipeline. On approval it places a hold on available credit.
Replays of the same request are idempotent.

## Requirements

### Requirement: Authorize Transaction
The system SHALL accept `POST /api/authorizations` with `requestId`, `cardId`, `amountCents`, `currency` and an optional `merchantName`. It SHALL respond `200 OK` with `requestId`, `decision` (`APPROVED` or `DECLINED`), `authorizationId` and `declineReason`. An approval SHALL set only `authorizationId`. A decline SHALL set only `declineReason`.

#### Scenario: Approved within available credit
- **GIVEN** an active, unexpired USD card with 10000 available
- **WHEN** a client authorizes 4000 USD
- **THEN** the response is `200` with `decision` `APPROVED`, a non-empty `authorizationId`, and no `declineReason`
- **AND** the card's available credit becomes 6000

#### Scenario: Declined is not an HTTP error
- **WHEN** an authorization is declined for any business reason
- **THEN** the response status is still `200 OK`, with `decision` `DECLINED` and no `authorizationId`
- **AND** available credit is unchanged

#### Scenario: Amount equal to available credit
- **WHEN** the amount equals the available credit exactly
- **THEN** the authorization is approved, and available credit becomes 0

### Requirement: Request Validation
The system SHALL respond `400 Bad Request` when `requestId`, `cardId` or `currency` is blank, when `amountCents` is missing, or when `currency` is not a valid ISO 4217 code. The API layer SHALL NOT reject an amount because of its sign. The domain declines non-positive amounts instead (see Rule Pipeline).

#### Scenario: Empty body
- **WHEN** a client posts `{}`
- **THEN** the response status is `400 Bad Request`

### Requirement: Card Lookup
The system SHALL decline with `CARD_NOT_FOUND` when no account exists for `cardId`. It SHALL do this before evaluating any rule.

#### Scenario: Unknown card
- **WHEN** a client authorizes against a card that does not exist
- **THEN** `decision` is `DECLINED` with `declineReason` `CARD_NOT_FOUND`

### Requirement: Rule Pipeline
The system SHALL evaluate the rules in this order. The first rule that objects SHALL decide the decline reason, and later rules SHALL NOT run:

1. `AmountPositiveRule`: `INVALID_AMOUNT`
2. `CardStatusRule`: `CARD_NOT_ACTIVE`
3. `CardExpiryRule`: `CARD_EXPIRED`
4. `CurrencyMatchRule`: `CURRENCY_MISMATCH`
5. `SufficientCreditRule`: `INSUFFICIENT_CREDIT`

#### Scenario: Non-positive amount
- **WHEN** `amountCents` is 0 or negative
- **THEN** the decline reason is `INVALID_AMOUNT`

#### Scenario: Frozen or closed card
- **WHEN** the card status is `FROZEN` or `CLOSED`
- **THEN** the decline reason is `CARD_NOT_ACTIVE`

#### Scenario: Card valid through end of expiration month
- **GIVEN** a card expiring in the current month
- **WHEN** a client authorizes on the last day of that month
- **THEN** the card is not declined for expiry
- **AND** on the first day of the next month, the decline reason is `CARD_EXPIRED`

#### Scenario: Currency mismatch
- **WHEN** a client authorizes EUR against a USD card
- **THEN** the decline reason is `CURRENCY_MISMATCH`

#### Scenario: Insufficient credit
- **WHEN** the amount exceeds the available credit
- **THEN** the decline reason is `INSUFFICIENT_CREDIT`

#### Scenario: First failing rule wins
- **GIVEN** a frozen card with insufficient credit
- **WHEN** a client authorizes against it
- **THEN** the decline reason is `CARD_NOT_ACTIVE`, not `INSUFFICIENT_CREDIT`

### Requirement: Extensible Rules
The system SHALL allow new rules to be added by implementing `AuthorizationRule`, which takes an `AuthorizationContext` (request, account, today) and returns an optional `DeclineReason`. Rules SHALL be supplied to `AuthorizationService` as an ordered list.

#### Scenario: Custom rule declines
- **GIVEN** the pipeline includes a custom rule that objects to amounts over 50000
- **WHEN** a client authorizes 60000 on a card with enough credit
- **THEN** the decline reason is the one the custom rule returns

### Requirement: Approval Places Hold
On approval, the system SHALL reduce the card's available credit by the amount. It SHALL generate a unique `authorizationId` and record an active `Hold` (`authorizationId`, `cardId`, `amount`) for that ID.

#### Scenario: Sequential approvals see reduced credit
- **GIVEN** a card with 10000 available
- **WHEN** a client approves 6000 and then requests 6000 under a new `requestId`
- **THEN** the second request is declined with `INSUFFICIENT_CREDIT`

### Requirement: Idempotent Requests
The system SHALL treat `requestId` as an idempotency key. If a `requestId` was already processed, the system SHALL return the stored original result without re-evaluating rules or placing another hold. This applies to both approvals and declines.

#### Scenario: Replayed approval holds once
- **WHEN** the same approved request is submitted twice
- **THEN** both responses are `APPROVED` with the same `authorizationId`
- **AND** available credit is reduced only once

#### Scenario: Replayed decline stays declined
- **GIVEN** a request was declined for `INSUFFICIENT_CREDIT`
- **AND** credit has since been freed
- **WHEN** a client resubmits the same `requestId`
- **THEN** the original `DECLINED` result is returned

### Requirement: Serialized Processing
The system SHALL run authorizations and reversals one at a time, so that concurrent requests cannot hold more credit than is available.

#### Scenario: Concurrent requests
- **WHEN** two requests for 6000 arrive concurrently on a card with 10000 available
- **THEN** exactly one request is approved
