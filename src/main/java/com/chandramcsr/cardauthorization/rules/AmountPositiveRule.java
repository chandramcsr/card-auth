package com.chandramcsr.cardauthorization.rules;

import com.chandramcsr.cardauthorization.model.DeclineReason;

import java.util.Optional;

public class AmountPositiveRule implements AuthorizationRule {

    @Override
    public Optional<DeclineReason> evaluate(AuthorizationContext context) {
        return context.request().amount().isPositive()
                ? Optional.empty()
                : Optional.of(DeclineReason.INVALID_AMOUNT);
    }
}
