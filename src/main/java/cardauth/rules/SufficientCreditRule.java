package cardauth.rules;

import cardauth.model.DeclineReason;

import java.util.Optional;

public class SufficientCreditRule implements AuthorizationRule {

    @Override
    public Optional<DeclineReason> evaluate(AuthorizationContext context) {
        boolean enough = context.account().availableCredit()
                .compareTo(context.request().amount()) >= 0;
        return enough ? Optional.empty() : Optional.of(DeclineReason.INSUFFICIENT_CREDIT);
    }
}
