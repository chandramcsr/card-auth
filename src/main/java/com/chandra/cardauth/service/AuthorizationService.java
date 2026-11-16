package com.chandra.cardauth.service;

import com.chandra.cardauth.model.AuthorizationRequest;
import com.chandra.cardauth.model.AuthorizationResult;
import com.chandra.cardauth.model.CardAccount;
import com.chandra.cardauth.model.DeclineReason;
import com.chandra.cardauth.model.Hold;
import com.chandra.cardauth.repository.AuthorizationRepository;
import com.chandra.cardauth.repository.CardAccountRepository;
import com.chandra.cardauth.rules.AuthorizationContext;
import com.chandra.cardauth.rules.AuthorizationRule;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Authorizes card transactions by running a pipeline of {@link AuthorizationRule}s and, on
 * approval, placing a hold against the account's available credit.
 *
 * <p>Operations are serialized with a single lock for simplicity. With a real datastore this
 * becomes a per-account lock or an optimistic-concurrency check on the account row.
 */
public class AuthorizationService {

    private final CardAccountRepository cards;
    private final AuthorizationRepository authorizations;
    private final List<AuthorizationRule> rules;
    private final Clock clock;
    private final Supplier<String> idGenerator;

    public AuthorizationService(CardAccountRepository cards, AuthorizationRepository authorizations,
                                List<AuthorizationRule> rules, Clock clock) {
        this(cards, authorizations, rules, clock, () -> UUID.randomUUID().toString());
    }

    public AuthorizationService(CardAccountRepository cards, AuthorizationRepository authorizations,
                                List<AuthorizationRule> rules, Clock clock,
                                Supplier<String> idGenerator) {
        this.cards = Objects.requireNonNull(cards);
        this.authorizations = Objects.requireNonNull(authorizations);
        this.rules = List.copyOf(rules);
        this.clock = Objects.requireNonNull(clock);
        this.idGenerator = Objects.requireNonNull(idGenerator);
    }

    public synchronized AuthorizationResult authorize(AuthorizationRequest request) {
        Objects.requireNonNull(request, "request");

        Optional<AuthorizationResult> previous =
                authorizations.findResultByRequestId(request.requestId());
        if (previous.isPresent()) {
            return previous.get();
        }

        AuthorizationResult result = decide(request);
        authorizations.saveResult(result);
        return result;
    }

    /** Releases the hold for an approved authorization. Reversing twice has no further effect. */
    public synchronized void reverse(String authorizationId) {
        Hold hold = authorizations.findHold(authorizationId)
                .orElseThrow(() -> new UnknownAuthorizationException(authorizationId));
        if (hold.released()) {
            return;
        }
        CardAccount account = cards.findById(hold.cardId())
                .orElseThrow(() -> new IllegalStateException("Card missing for hold " + authorizationId));
        account.release(hold.amount());
        cards.save(account);
        authorizations.saveHold(hold.asReleased());
    }

    private AuthorizationResult decide(AuthorizationRequest request) {
        Optional<CardAccount> found = cards.findById(request.cardId());
        if (found.isEmpty()) {
            return AuthorizationResult.declined(request.requestId(), DeclineReason.CARD_NOT_FOUND);
        }
        CardAccount account = found.get();

        AuthorizationContext context =
                new AuthorizationContext(request, account, LocalDate.now(clock));
        for (AuthorizationRule rule : rules) {
            Optional<DeclineReason> objection = rule.evaluate(context);
            if (objection.isPresent()) {
                return AuthorizationResult.declined(request.requestId(), objection.get());
            }
        }

        account.hold(request.amount());
        cards.save(account);

        String authorizationId = idGenerator.get();
        authorizations.saveHold(new Hold(authorizationId, account.cardId(), request.amount(), false));
        return AuthorizationResult.approved(request.requestId(), authorizationId);
    }
}
