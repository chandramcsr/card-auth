# card-accounts Specification

## Purpose
This spec covers card accounts: creating them, looking them up, and tracking available credit
against a fixed credit limit. Amounts use the `Money` value type.

## Requirements

### Requirement: Create Card
The system SHALL create a card account when it receives `POST /api/cards` with `cardId`, `expiration` (`YYYY-MM`), `creditLimitCents` and `currency`. The new account SHALL have status `ACTIVE`, and its available credit SHALL equal its credit limit.

#### Scenario: Card created
- **WHEN** a client posts `{"cardId":"c1","expiration":"2099-12","creditLimitCents":10000,"currency":"USD"}`
- **THEN** the response status is `201 Created`
- **AND** the `Location` header is `/api/cards/c1`
- **AND** the body has `status` `ACTIVE`, `creditLimitCents` 10000 and `availableCreditCents` 10000

#### Scenario: Duplicate card id
- **WHEN** a client creates a card whose `cardId` already exists
- **THEN** the response status is `409 Conflict`
- **AND** the existing account is unchanged

#### Scenario: Invalid create payload
- **WHEN** `cardId` or `currency` is blank, `expiration` is missing or malformed, or `creditLimitCents` is missing or not positive
- **THEN** the response status is `400 Bad Request`

#### Scenario: Unknown currency code
- **WHEN** `currency` is not a valid ISO 4217 code
- **THEN** the response status is `400 Bad Request` with an RFC 7807 problem body

### Requirement: Get Card
The system SHALL return a card's `cardId`, `status`, `expiration`, `currency`, `creditLimitCents` and `availableCreditCents` when it receives `GET /api/cards/{cardId}`.

#### Scenario: Existing card
- **WHEN** a client requests an existing card
- **THEN** the response status is `200 OK`, and the body reflects the current available credit

#### Scenario: Unknown card
- **WHEN** a client requests a `cardId` that does not exist
- **THEN** the response status is `404 Not Found`

### Requirement: Available Credit Accounting
A card account SHALL reduce its available credit by the held amount when it places a hold. It SHALL NOT allow available credit to go below zero. When it releases a hold, it SHALL restore the amount, but available credit SHALL never exceed the credit limit.

#### Scenario: Hold within available credit
- **WHEN** a hold of 4000 is placed on an account with 10000 available
- **THEN** available credit becomes 6000

#### Scenario: Hold beyond available credit
- **WHEN** a hold larger than the available credit is placed
- **THEN** the operation fails, and available credit is unchanged

#### Scenario: Release is capped at the limit
- **WHEN** a release would raise available credit above the credit limit
- **THEN** available credit equals the credit limit

### Requirement: Money Semantics
The system SHALL represent amounts as whole numbers of minor units in a single ISO 4217 currency. Arithmetic and comparison between different currencies SHALL fail, and arithmetic overflow SHALL fail.

#### Scenario: Cross-currency arithmetic
- **WHEN** USD and EUR amounts are added, subtracted or compared
- **THEN** an `IllegalArgumentException` is thrown

#### Scenario: Overflow
- **WHEN** an addition exceeds the range of `long`
- **THEN** an `ArithmeticException` is thrown

### Requirement: Demo Seed Data
The system SHALL create the `demo-card` account (USD, 100000 cents limit, expiring 3 years from now) at startup when `card-authorization.seed-demo-card=true`. It SHALL NOT create it when the property is anything else.

#### Scenario: Seed enabled
- **WHEN** the application starts with the default properties
- **THEN** `GET /api/cards/demo-card` returns `ACTIVE` with `availableCreditCents` 100000
