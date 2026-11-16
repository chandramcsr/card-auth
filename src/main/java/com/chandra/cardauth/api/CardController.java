package com.chandra.cardauth.api;

import com.chandra.cardauth.model.CardAccount;
import com.chandra.cardauth.model.Money;
import com.chandra.cardauth.repository.CardAccountRepository;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardAccountRepository cards;

    public CardController(CardAccountRepository cards) {
        this.cards = cards;
    }

    @PostMapping("/create")
    public ResponseEntity<CardResponse> create(@Valid @RequestBody CreateCardRequest request) {
        if (cards.findById(request.cardId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Card already exists: " + request.cardId());
        }
        CardAccount account = CardAccount.open(
                request.cardId(), request.expiration(), Money.of(request.currency(), request.creditLimitCents()));
        cards.save(account);
        return ResponseEntity.created(URI.create("/api/cards/" + account.cardId()))
                .body(CardResponse.from(account));
    }

    @GetMapping("/{cardId}")
    public CardResponse get(@PathVariable String cardId) {
        return cards.findById(cardId)
                .map(CardResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found: " + cardId));
    }
}
