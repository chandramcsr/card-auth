package com.chandramcsr.cardauthorization.model;

import java.util.Currency;
import java.util.Objects;

/** An amount in minor units (e.g. cents) of a single currency. Avoids floating-point error. */
public record Money(long minorUnits, Currency currency) implements Comparable<Money> {

    public Money {
        Objects.requireNonNull(currency, "currency");
    }

    public static Money of(String currencyCode, long minorUnits) {
        return new Money(minorUnits, Currency.getInstance(currencyCode));
    }

    public static Money usd(long cents) {
        return of("USD", cents);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(minorUnits, other.minorUnits), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.subtractExact(minorUnits, other.minorUnits), currency);
    }

    public boolean isPositive() {
        return minorUnits > 0;
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return Long.compare(minorUnits, other.minorUnits);
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: " + currency + " vs " + other.currency);
        }
    }
}
