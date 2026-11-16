# authorization-reversal Specification

## Purpose
This spec covers releasing the hold that an approved authorization placed, which restores the
card's available credit.

## Requirements

### Requirement: Reverse Authorization
The system SHALL release the hold for `authorizationId` when it receives `POST /api/authorizations/{authorizationId}/reversal`. It SHALL restore the held amount to the card's available credit, capped at the credit limit, and respond `204 No Content`.

#### Scenario: Reversal restores credit
- **GIVEN** a card with 10000 limit and an approved 4000 authorization
- **WHEN** a client reverses the authorization
- **THEN** the response status is `204 No Content`
- **AND** available credit returns to 10000

### Requirement: Idempotent Reversal
The system SHALL treat a reversal of an already-released hold as a no-op that still succeeds. Credit SHALL be released at most once per hold.

#### Scenario: Reversing twice
- **WHEN** a client reverses the same authorization twice
- **THEN** both responses are `204 No Content`
- **AND** the amount is restored only once

### Requirement: Unknown Authorization
The system SHALL respond `404 Not Found` with an RFC 7807 problem body when no hold exists for `authorizationId`. This includes the IDs of declined requests, because declines create no hold.

#### Scenario: Unknown id
- **WHEN** a client reverses `nope`
- **THEN** the response status is `404 Not Found`
