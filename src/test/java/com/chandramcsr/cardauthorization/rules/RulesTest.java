package com.chandramcsr.cardauthorization.rules;

import static org.junit.jupiter.api.Assertions.*;

import com.chandramcsr.cardauthorization.model.AuthorizationRequest;
import com.chandramcsr.cardauthorization.model.CardAccount;
import com.chandramcsr.cardauthorization.model.CardStatus;
import com.chandramcsr.cardauthorization.model.DeclineReason;
import com.chandramcsr.cardauthorization.model.Money;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);

    private static AuthorizationContext context(CardAccount account, Money amount) {
        return new AuthorizationContext(
                new AuthorizationRequest("r1", account.cardId(), amount, "Shop"), account, TODAY);
    }

    private static CardAccount account(CardStatus status, YearMonth expiration, long limitCents) {
        return new CardAccount("c1", status, expiration, Money.usd(limitCents), Money.usd(limitCents));
    }

    @Test
    void amountPositiveRuleDeclinesZeroAndNegative() {
        CardAccount acct = account(CardStatus.ACTIVE, YearMonth.of(2030, 1), 100);
        AmountPositiveRule rule = new AmountPositiveRule();

        assertEquals(Optional.of(DeclineReason.INVALID_AMOUNT), rule.evaluate(context(acct, Money.usd(0))));
        assertEquals(Optional.of(DeclineReason.INVALID_AMOUNT), rule.evaluate(context(acct, Money.usd(-5))));
        assertTrue(rule.evaluate(context(acct, Money.usd(1))).isEmpty());
    }

    @Test
    void cardStatusRuleOnlyAllowsActive() {
        CardStatusRule rule = new CardStatusRule();
        YearMonth exp = YearMonth.of(2030, 1);

        assertTrue(rule.evaluate(context(account(CardStatus.ACTIVE, exp, 100), Money.usd(1))).isEmpty());
        assertEquals(Optional.of(DeclineReason.CARD_NOT_ACTIVE),
                rule.evaluate(context(account(CardStatus.FROZEN, exp, 100), Money.usd(1))));
        assertEquals(Optional.of(DeclineReason.CARD_NOT_ACTIVE),
                rule.evaluate(context(account(CardStatus.CLOSED, exp, 100), Money.usd(1))));
    }

    @Test
    void cardExpiryRuleIsValidThroughEndOfExpirationMonth() {
        CardExpiryRule rule = new CardExpiryRule();

        CardAccount lastValidMonth = account(CardStatus.ACTIVE, YearMonth.of(2026, 11), 100);
        CardAccount monthBefore = account(CardStatus.ACTIVE, YearMonth.of(2026, 10), 100);

        assertTrue(rule.evaluate(context(lastValidMonth, Money.usd(1))).isEmpty());
        assertEquals(Optional.of(DeclineReason.CARD_EXPIRED),
                rule.evaluate(context(monthBefore, Money.usd(1))));
    }

    @Test
    void currencyMatchRuleDeclinesForeignCurrency() {
        CardAccount acct = account(CardStatus.ACTIVE, YearMonth.of(2030, 1), 100);
        CurrencyMatchRule rule = new CurrencyMatchRule();

        assertTrue(rule.evaluate(context(acct, Money.usd(1))).isEmpty());
        assertEquals(Optional.of(DeclineReason.CURRENCY_MISMATCH),
                rule.evaluate(context(acct, Money.of("EUR", 1))));
    }

    @Test
    void sufficientCreditRuleAllowsExactlyTheAvailableAmount() {
        CardAccount acct = account(CardStatus.ACTIVE, YearMonth.of(2030, 1), 100);
        SufficientCreditRule rule = new SufficientCreditRule();

        assertTrue(rule.evaluate(context(acct, Money.usd(100))).isEmpty());
        assertEquals(Optional.of(DeclineReason.INSUFFICIENT_CREDIT),
                rule.evaluate(context(acct, Money.usd(101))));
    }

    @Test
    void defaultsRunStructuralChecksBeforeBalanceCheck() {
        var defaults = AuthorizationRules.defaults();
        assertInstanceOf(AmountPositiveRule.class, defaults.get(0));
        assertInstanceOf(SufficientCreditRule.class, defaults.get(defaults.size() - 1));
    }
}
