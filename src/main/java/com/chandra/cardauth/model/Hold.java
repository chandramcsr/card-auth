package com.chandra.cardauth.model;

/** Credit reserved by an approved authorization until it is reversed (or, later, captured). */
public record Hold(String authorizationId, String cardId, Money amount, boolean released) {

    public Hold asReleased() {
        return new Hold(authorizationId, cardId, amount, true);
    }
}
