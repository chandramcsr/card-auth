package cardauth.api;

import cardauth.model.AuthorizationResult;
import cardauth.model.DeclineReason;

public record AuthorizationResponse(String requestId, AuthorizationResult.Decision decision,
                                    String authorizationId, DeclineReason declineReason) {

    public static AuthorizationResponse from(AuthorizationResult result) {
        return new AuthorizationResponse(result.requestId(), result.decision(),
                result.authorizationId(), result.declineReason());
    }
}
