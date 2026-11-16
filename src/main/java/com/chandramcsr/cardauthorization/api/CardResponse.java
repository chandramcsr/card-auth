package com.chandramcsr.cardauthorization.api;

import com.chandramcsr.cardauthorization.model.CardAccount;
import com.chandramcsr.cardauthorization.model.CardStatus;
import java.time.YearMonth;

public record CardResponse(String cardId, CardStatus status, YearMonth expiration,
                           String currency, long creditLimitCents, long availableCreditCents) {

    public static CardResponse from(CardAccount account) {
        return new CardResponse(
                account.cardId(),
                account.status(),
                account.expiration(),
                account.creditLimit().currency().getCurrencyCode(),
                account.creditLimit().minorUnits(),
                account.availableCredit().minorUnits());
    }
}
