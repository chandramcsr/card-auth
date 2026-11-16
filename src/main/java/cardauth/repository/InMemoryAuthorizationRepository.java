package cardauth.repository;

import cardauth.model.AuthorizationResult;
import cardauth.model.Hold;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAuthorizationRepository implements AuthorizationRepository {

    private final Map<String, AuthorizationResult> resultsByRequestId = new ConcurrentHashMap<>();
    private final Map<String, Hold> holdsByAuthorizationId = new ConcurrentHashMap<>();

    @Override
    public Optional<AuthorizationResult> findResultByRequestId(String requestId) {
        return Optional.ofNullable(resultsByRequestId.get(requestId));
    }

    @Override
    public void saveResult(AuthorizationResult result) {
        resultsByRequestId.put(result.requestId(), result);
    }

    @Override
    public Optional<Hold> findHold(String authorizationId) {
        return Optional.ofNullable(holdsByAuthorizationId.get(authorizationId));
    }

    @Override
    public void saveHold(Hold hold) {
        holdsByAuthorizationId.put(hold.authorizationId(), hold);
    }
}
