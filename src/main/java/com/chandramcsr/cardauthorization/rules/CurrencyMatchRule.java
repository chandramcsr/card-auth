package com.chandramcsr.cardauthorization.rules;

import com.chandramcsr.cardauthorization.model.DeclineReason;

import java.util.Optional;

/** Transactions must be in the account's currency (no FX conversion yet). */
public class CurrencyMatchRule implements AuthorizationRule {

    @Override
    public Optional<DeclineReason> evaluate(AuthorizationContext context) {
        boolean same = context.request().amount().currency()
                .equals(context.account().creditLimit().currency());
        return same ? Optional.empty() : Optional.of(DeclineReason.CURRENCY_MISMATCH);
    }
}
