package cardauth.model;

import java.time.YearMonth;
import java.util.Objects;

/**
 * A credit card account. Tracks the credit limit and the credit still available after
 * pending authorization holds. Not thread-safe on its own; callers coordinate access.
 */
public class CardAccount {

    private final String cardId;
    private final YearMonth expiration;
    private final Money creditLimit;
    private CardStatus status;
    private Money availableCredit;

    public CardAccount(String cardId, CardStatus status, YearMonth expiration,
                       Money creditLimit, Money availableCredit) {
        this.cardId = requireNonBlank(cardId);
        this.status = Objects.requireNonNull(status, "status");
        this.expiration = Objects.requireNonNull(expiration, "expiration");
        this.creditLimit = Objects.requireNonNull(creditLimit, "creditLimit");
        this.availableCredit = Objects.requireNonNull(availableCredit, "availableCredit");
    }

    /** Opens an active account with its full limit available. */
    public static CardAccount open(String cardId, YearMonth expiration, Money creditLimit) {
        return new CardAccount(cardId, CardStatus.ACTIVE, expiration, creditLimit, creditLimit);
    }

    /** Reserves credit for an approved authorization. */
    public void hold(Money amount) {
        if (availableCredit.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient available credit on " + cardId);
        }
        availableCredit = availableCredit.minus(amount);
    }

    /** Returns previously held credit, never exceeding the credit limit. */
    public void release(Money amount) {
        Money restored = availableCredit.plus(amount);
        availableCredit = restored.compareTo(creditLimit) > 0 ? creditLimit : restored;
    }

    public String cardId() { return cardId; }
    public CardStatus status() { return status; }
    public YearMonth expiration() { return expiration; }
    public Money creditLimit() { return creditLimit; }
    public Money availableCredit() { return availableCredit; }

    public void setStatus(CardStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    private static String requireNonBlank(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("cardId must not be blank");
        }
        return value;
    }
}
