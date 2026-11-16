package com.chandramcsr.cardauthorization.model;

public enum DeclineReason {
    CARD_NOT_FOUND,
    INVALID_AMOUNT,
    CARD_NOT_ACTIVE,
    CARD_EXPIRED,
    CURRENCY_MISMATCH,
    INSUFFICIENT_CREDIT
}
