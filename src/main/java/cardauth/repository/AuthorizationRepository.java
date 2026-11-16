package cardauth.repository;

import cardauth.model.AuthorizationResult;
import cardauth.model.Hold;

import java.util.Optional;

/** Stores authorization outcomes (for idempotent replays) and the holds they created. */
public interface AuthorizationRepository {

    Optional<AuthorizationResult> findResultByRequestId(String requestId);

    void saveResult(AuthorizationResult result);

    Optional<Hold> findHold(String authorizationId);

    void saveHold(Hold hold);
}
