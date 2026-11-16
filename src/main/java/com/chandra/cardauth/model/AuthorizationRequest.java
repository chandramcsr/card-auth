package com.chandra.cardauth.model;

import java.util.Objects;

/**
 * A request to authorize a purchase. The requestId is the idempotency key: resubmitting the
 * same requestId returns the original result instead of holding funds twice.
 */
public record AuthorizationRequest(String requestId, String cardId, Money amount, String merchantName) {

    public AuthorizationRequest {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must not be blank");
        }
        if (cardId == null || cardId.isBlank()) {
            throw new IllegalArgumentException("cardId must not be blank");
        }
        Objects.requireNonNull(amount, "amount");
    }
}
