package cardauth.api;

import cardauth.repository.AuthorizationRepository;
import cardauth.repository.CardAccountRepository;
import cardauth.repository.InMemoryAuthorizationRepository;
import cardauth.repository.InMemoryCardAccountRepository;
import cardauth.rules.AuthorizationRules;
import cardauth.service.AuthorizationService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the framework-free authorization core into the Spring context. */
@Configuration
public class AuthorizationConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    CardAccountRepository cardAccountRepository() {
        return new InMemoryCardAccountRepository();
    }

    @Bean
    AuthorizationRepository authorizationRepository() {
        return new InMemoryAuthorizationRepository();
    }

    @Bean
    AuthorizationService authorizationService(CardAccountRepository cards,
                                              AuthorizationRepository authorizations,
                                              Clock clock) {
        return new AuthorizationService(cards, authorizations, AuthorizationRules.defaults(), clock);
    }
}
