package cardauth.service;

public class UnknownAuthorizationException extends RuntimeException {

    public UnknownAuthorizationException(String authorizationId) {
        super("No authorization found with id " + authorizationId);
    }
}
