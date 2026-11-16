package cardauth.rules;

import cardauth.model.CardStatus;
import cardauth.model.DeclineReason;

import java.util.Optional;

public class CardStatusRule implements AuthorizationRule {

    @Override
    public Optional<DeclineReason> evaluate(AuthorizationContext context) {
        return context.account().status() == CardStatus.ACTIVE
                ? Optional.empty()
                : Optional.of(DeclineReason.CARD_NOT_ACTIVE);
    }
}
