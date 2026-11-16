package com.chandramcsr.cardauthorization.rules;

import com.chandramcsr.cardauthorization.model.AuthorizationRequest;
import com.chandramcsr.cardauthorization.model.CardAccount;

import java.time.LocalDate;

/** Everything a rule needs to make a decision. Add fields here as new rules need them. */
public record AuthorizationContext(AuthorizationRequest request, CardAccount account, LocalDate today) {
}
