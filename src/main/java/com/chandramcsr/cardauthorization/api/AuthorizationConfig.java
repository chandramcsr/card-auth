package com.chandramcsr.cardauthorization.api;

import com.chandramcsr.cardauthorization.repository.AuthorizationRepository;
import com.chandramcsr.cardauthorization.repository.CardAccountRepository;
import com.chandramcsr.cardauthorization.repository.InMemoryAuthorizationRepository;
import com.chandramcsr.cardauthorization.repository.InMemoryCardAccountRepository;
import com.chandramcsr.cardauthorization.rules.AuthorizationRules;
import com.chandramcsr.cardauthorization.service.AuthorizationService;
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
