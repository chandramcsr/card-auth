package com.chandramcsr.cardauthorization.rules;

import com.chandramcsr.cardauthorization.model.CardStatus;
import com.chandramcsr.cardauthorization.model.DeclineReason;

import java.util.Optional;

public class CardStatusRule implements AuthorizationRule {

    @Override
    public Optional<DeclineReason> evaluate(AuthorizationContext context) {
        return context.account().status() == CardStatus.ACTIVE
                ? Optional.empty()
                : Optional.of(DeclineReason.CARD_NOT_ACTIVE);
    }
}
