package cardauth.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void addsAndSubtractsInMinorUnits() {
        assertEquals(Money.usd(1_50), Money.usd(1_00).plus(Money.usd(50)));
        assertEquals(Money.usd(50), Money.usd(1_00).minus(Money.usd(50)));
    }

    @Test
    void comparesAmountsInSameCurrency() {
        assertTrue(Money.usd(100).compareTo(Money.usd(99)) > 0);
        assertEquals(0, Money.usd(100).compareTo(Money.usd(100)));
        assertTrue(Money.usd(99).compareTo(Money.usd(100)) < 0);
    }

    @Test
    void rejectsArithmeticAcrossCurrencies() {
        assertThrows(IllegalArgumentException.class, () -> Money.usd(100).plus(Money.of("EUR", 100)));
        assertThrows(IllegalArgumentException.class, () -> Money.usd(100).minus(Money.of("EUR", 100)));
        assertThrows(IllegalArgumentException.class, () -> Money.usd(100).compareTo(Money.of("EUR", 100)));
    }

    @Test
    void isPositiveOnlyForAmountsAboveZero() {
        assertTrue(Money.usd(1).isPositive());
        assertFalse(Money.usd(0).isPositive());
        assertFalse(Money.usd(-1).isPositive());
    }

    @Test
    void overflowIsNotSilent() {
        assertThrows(ArithmeticException.class, () -> Money.usd(Long.MAX_VALUE).plus(Money.usd(1)));
    }
}
