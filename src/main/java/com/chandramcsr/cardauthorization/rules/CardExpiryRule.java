package com.chandramcsr.cardauthorization.rules;

import com.chandramcsr.cardauthorization.model.DeclineReason;

import java.time.YearMonth;
import java.util.Optional;

/** A card is valid through the last day of its expiration month. */
public class CardExpiryRule implements AuthorizationRule {

    @Override
    public Optional<DeclineReason> evaluate(AuthorizationContext context) {
        boolean expired = YearMonth.from(context.today()).isAfter(context.account().expiration());
        return expired ? Optional.of(DeclineReason.CARD_EXPIRED) : Optional.empty();
    }
}
