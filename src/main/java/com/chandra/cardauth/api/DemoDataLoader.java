package com.chandra.cardauth.api;

import com.chandra.cardauth.model.CardAccount;
import com.chandra.cardauth.model.Money;
import com.chandra.cardauth.repository.CardAccountRepository;
import java.time.YearMonth;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Seeds one card ("demo-card", $1,000 limit) so the API can be exercised immediately. */
@Component
@ConditionalOnProperty(name = "card-authorization.seed-demo-card", havingValue = "true")
public class DemoDataLoader implements CommandLineRunner {

    private final CardAccountRepository cards;

    public DemoDataLoader(CardAccountRepository cards) {
        this.cards = cards;
    }

    @Override
    public void run(String... args) {
        cards.save(CardAccount.open("demo-card", YearMonth.now().plusYears(3), Money.usd(1_000_00)));
    }
}
