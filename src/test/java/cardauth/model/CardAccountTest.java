package cardauth.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class CardAccountTest {

    private final CardAccount account =
            CardAccount.open("card-1", YearMonth.of(2030, 1), Money.usd(100_00));

    @Test
    void openStartsActiveWithFullLimitAvailable() {
        assertEquals(CardStatus.ACTIVE, account.status());
        assertEquals(Money.usd(100_00), account.availableCredit());
    }

    @Test
    void holdReducesAvailableCredit() {
        account.hold(Money.usd(30_00));
        assertEquals(Money.usd(70_00), account.availableCredit());
    }

    @Test
    void holdBeyondAvailableCreditFails() {
        assertThrows(IllegalStateException.class, () -> account.hold(Money.usd(100_01)));
        assertEquals(Money.usd(100_00), account.availableCredit());
    }

    @Test
    void releaseRestoresCreditButNeverExceedsLimit() {
        account.hold(Money.usd(30_00));
        account.release(Money.usd(30_00));
        assertEquals(Money.usd(100_00), account.availableCredit());

        account.release(Money.usd(500_00));
        assertEquals(Money.usd(100_00), account.availableCredit());
    }

    @Test
    void rejectsBlankCardId() {
        assertThrows(IllegalArgumentException.class,
                () -> CardAccount.open(" ", YearMonth.of(2030, 1), Money.usd(1)));
    }
}
