package cardauth.service;

import static cardauth.model.DeclineReason.*;
import static org.junit.jupiter.api.Assertions.*;

import cardauth.model.AuthorizationRequest;
import cardauth.model.AuthorizationResult;
import cardauth.model.CardAccount;
import cardauth.model.CardStatus;
import cardauth.model.DeclineReason;
import cardauth.model.Money;
import cardauth.repository.InMemoryAuthorizationRepository;
import cardauth.repository.InMemoryCardAccountRepository;
import cardauth.rules.AuthorizationRule;
import cardauth.rules.AuthorizationRules;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthorizationServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC);
    private static final String CARD = "card-1";

    private InMemoryCardAccountRepository cards;
    private InMemoryAuthorizationRepository authorizations;
    private AuthorizationService service;
    private int requestCounter;

    @BeforeEach
    void setUp() {
        cards = new InMemoryCardAccountRepository();
        authorizations = new InMemoryAuthorizationRepository();
        cards.save(CardAccount.open(CARD, YearMonth.of(2030, 12), Money.usd(100_00)));
        service = serviceWith(AuthorizationRules.defaults());
        requestCounter = 0;
    }

    private AuthorizationService serviceWith(List<AuthorizationRule> rules) {
        AtomicInteger ids = new AtomicInteger();
        return new AuthorizationService(cards, authorizations, rules, CLOCK,
                () -> "auth-" + ids.incrementAndGet());
    }

    private AuthorizationResult authorize(String cardId, Money amount) {
        return service.authorize(new AuthorizationRequest("req-" + (++requestCounter), cardId, amount, "Shop"));
    }

    private Money available() {
        return cards.findById(CARD).orElseThrow().availableCredit();
    }

    private void assertDeclined(DeclineReason expected, AuthorizationResult result) {
        assertFalse(result.isApproved());
        assertEquals(expected, result.declineReason());
        assertNull(result.authorizationId());
    }

    // ---- approvals ----

    @Test
    void approvesWithinLimitAndHoldsFunds() {
        AuthorizationResult result = authorize(CARD, Money.usd(40_00));

        assertTrue(result.isApproved());
        assertEquals("auth-1", result.authorizationId());
        assertNull(result.declineReason());
        assertEquals(Money.usd(60_00), available());
    }

    @Test
    void approvesAmountEqualToAvailableCredit() {
        assertTrue(authorize(CARD, Money.usd(100_00)).isApproved());
        assertEquals(Money.usd(0), available());
    }

    @Test
    void secondAuthorizationSeesCreditReducedByFirst() {
        assertTrue(authorize(CARD, Money.usd(60_00)).isApproved());
        assertDeclined(INSUFFICIENT_CREDIT, authorize(CARD, Money.usd(60_00)));
        assertTrue(authorize(CARD, Money.usd(40_00)).isApproved());
    }

    // ---- declines ----

    @Test
    void declinesAmountOverAvailableCreditWithoutHolding() {
        assertDeclined(INSUFFICIENT_CREDIT, authorize(CARD, Money.usd(100_01)));
        assertEquals(Money.usd(100_00), available());
    }

    @Test
    void declinesUnknownCard() {
        assertDeclined(CARD_NOT_FOUND, authorize("missing", Money.usd(1_00)));
    }

    @Test
    void declinesNonPositiveAmounts() {
        assertDeclined(INVALID_AMOUNT, authorize(CARD, Money.usd(0)));
        assertDeclined(INVALID_AMOUNT, authorize(CARD, Money.usd(-1_00)));
    }

    @Test
    void declinesFrozenAndClosedCards() {
        cards.findById(CARD).orElseThrow().setStatus(CardStatus.FROZEN);
        assertDeclined(CARD_NOT_ACTIVE, authorize(CARD, Money.usd(1_00)));

        cards.findById(CARD).orElseThrow().setStatus(CardStatus.CLOSED);
        assertDeclined(CARD_NOT_ACTIVE, authorize(CARD, Money.usd(1_00)));
    }

    @Test
    void declinesExpiredCardButAcceptsCardExpiringThisMonth() {
        cards.save(CardAccount.open("expired", YearMonth.of(2026, 10), Money.usd(100_00)));
        cards.save(CardAccount.open("expiring", YearMonth.of(2026, 11), Money.usd(100_00)));

        assertDeclined(CARD_EXPIRED, authorize("expired", Money.usd(1_00)));
        assertTrue(authorize("expiring", Money.usd(1_00)).isApproved());
    }

    @Test
    void declinesCurrencyMismatch() {
        assertDeclined(CURRENCY_MISMATCH, authorize(CARD, Money.of("EUR", 1_00)));
    }

    @Test
    void firstFailingRuleDeterminesReason() {
        cards.findById(CARD).orElseThrow().setStatus(CardStatus.FROZEN);
        // Frozen AND over limit: status is checked before credit.
        assertDeclined(CARD_NOT_ACTIVE, authorize(CARD, Money.usd(500_00)));
    }

    // ---- idempotency ----

    @Test
    void replayedRequestReturnsOriginalResultAndHoldsOnce() {
        AuthorizationRequest request = new AuthorizationRequest("dup", CARD, Money.usd(30_00), "Shop");

        AuthorizationResult first = service.authorize(request);
        AuthorizationResult second = service.authorize(request);

        assertEquals(first, second);
        assertEquals(Money.usd(70_00), available());
    }

    @Test
    void replayedDeclineStaysDeclinedEvenIfCreditFreesUp() {
        AuthorizationResult big = authorize(CARD, Money.usd(100_00));
        AuthorizationRequest declined = new AuthorizationRequest("late", CARD, Money.usd(50_00), "Shop");
        assertEquals(INSUFFICIENT_CREDIT, service.authorize(declined).declineReason());

        service.reverse(big.authorizationId());

        assertEquals(INSUFFICIENT_CREDIT, service.authorize(declined).declineReason());
    }

    // ---- reversal ----

    @Test
    void reversalRestoresAvailableCredit() {
        AuthorizationResult result = authorize(CARD, Money.usd(40_00));
        service.reverse(result.authorizationId());
        assertEquals(Money.usd(100_00), available());
    }

    @Test
    void reversingTwiceDoesNotReleaseTwice() {
        AuthorizationResult a = authorize(CARD, Money.usd(40_00));
        authorize(CARD, Money.usd(20_00));

        service.reverse(a.authorizationId());
        service.reverse(a.authorizationId());

        assertEquals(Money.usd(80_00), available());
    }

    @Test
    void reversingUnknownAuthorizationFails() {
        assertThrows(UnknownAuthorizationException.class, () -> service.reverse("nope"));
    }

    // ---- extensibility ----

    @Test
    void customRuleCanBeAddedToThePipeline() {
        AuthorizationRule blockLargeMerchantPurchases = ctx ->
                ctx.request().amount().compareTo(Money.usd(50_00)) > 0
                        ? Optional.of(INSUFFICIENT_CREDIT)
                        : Optional.empty();
        List<AuthorizationRule> rules = new ArrayList<>(AuthorizationRules.defaults());
        rules.add(0, blockLargeMerchantPurchases);
        service = serviceWith(rules);

        assertDeclined(INSUFFICIENT_CREDIT, authorize(CARD, Money.usd(50_01)));
        assertTrue(authorize(CARD, Money.usd(50_00)).isApproved());
    }

    @Test
    void emptyRuleSetApprovesAnythingForKnownCardUntilHoldFails() {
        service = serviceWith(List.of());
        assertTrue(authorize(CARD, Money.usd(10_00)).isApproved());
    }

    // ---- input validation ----

    @Test
    void requestRejectsBlankIdsAndNullAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuthorizationRequest(" ", CARD, Money.usd(1), "Shop"));
        assertThrows(IllegalArgumentException.class,
                () -> new AuthorizationRequest("r", "", Money.usd(1), "Shop"));
        assertThrows(NullPointerException.class,
                () -> new AuthorizationRequest("r", CARD, null, "Shop"));
    }

    @Test
    void nullRequestIsRejected() {
        assertThrows(NullPointerException.class, () -> service.authorize(null));
    }
}
