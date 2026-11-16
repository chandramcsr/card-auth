package cardauth.rules;

import java.util.List;

public final class AuthorizationRules {

    private AuthorizationRules() { }

    /**
     * The standard rule set, in evaluation order. The first rule that objects decides the
     * decline reason, so cheap structural checks run before balance checks.
     */
    public static List<AuthorizationRule> defaults() {
        return List.of(
                new AmountPositiveRule(),
                new CardStatusRule(),
                new CardExpiryRule(),
                new CurrencyMatchRule(),
                new SufficientCreditRule());
    }
}
