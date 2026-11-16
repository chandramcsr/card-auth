package com.chandra.cardauth.rules;

import com.chandra.cardauth.model.AuthorizationRequest;
import com.chandra.cardauth.model.CardAccount;

import java.time.LocalDate;

/** Everything a rule needs to make a decision. Add fields here as new rules need them. */
public record AuthorizationContext(AuthorizationRequest request, CardAccount account, LocalDate today) {
}
