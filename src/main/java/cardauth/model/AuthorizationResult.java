package cardauth.model;

/** Outcome of an authorization. Exactly one of authorizationId / declineReason is set. */
public record AuthorizationResult(String requestId, Decision decision,
                                  String authorizationId, DeclineReason declineReason) {

    public enum Decision { APPROVED, DECLINED }

    public static AuthorizationResult approved(String requestId, String authorizationId) {
        return new AuthorizationResult(requestId, Decision.APPROVED, authorizationId, null);
    }

    public static AuthorizationResult declined(String requestId, DeclineReason reason) {
        return new AuthorizationResult(requestId, Decision.DECLINED, null, reason);
    }

    public boolean isApproved() {
        return decision == Decision.APPROVED;
    }
}
