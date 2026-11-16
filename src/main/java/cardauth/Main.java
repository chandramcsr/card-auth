package cardauth;

import cardauth.model.AuthorizationRequest;
import cardauth.model.AuthorizationResult;
import cardauth.model.CardAccount;
import cardauth.model.Money;
import cardauth.repository.InMemoryAuthorizationRepository;
import cardauth.repository.InMemoryCardAccountRepository;
import cardauth.rules.AuthorizationRules;
import cardauth.service.AuthorizationService;

import java.time.Clock;
import java.time.YearMonth;

/** Small demo wiring the service together. */
public class Main {

    public static void main(String[] args) {
        InMemoryCardAccountRepository cards = new InMemoryCardAccountRepository();
        cards.save(CardAccount.open("card-1", YearMonth.of(2030, 12), Money.usd(100_00)));

        AuthorizationService service = new AuthorizationService(
                cards, new InMemoryAuthorizationRepository(),
                AuthorizationRules.defaults(), Clock.systemUTC());

        print(service.authorize(new AuthorizationRequest("r1", "card-1", Money.usd(60_00), "Coffee Co")));
        print(service.authorize(new AuthorizationRequest("r2", "card-1", Money.usd(60_00), "Book Store")));
        print(service.authorize(new AuthorizationRequest("r3", "card-1", Money.usd(40_00), "Gas Station")));
    }

    private static void print(AuthorizationResult result) {
        System.out.println(result);
    }
}
