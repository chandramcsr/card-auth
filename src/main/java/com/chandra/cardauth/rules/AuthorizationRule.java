package com.chandra.cardauth.rules;

import com.chandra.cardauth.model.DeclineReason;

import java.util.Optional;

/**
 * A single check in the authorization pipeline. Returns a decline reason if the
 * transaction must be declined, or empty if this rule has no objection.
 * New business rules (velocity limits, MCC blocks, fraud scores) implement this interface.
 */
@FunctionalInterface
public interface AuthorizationRule {

    Optional<DeclineReason> evaluate(AuthorizationContext context);
}
