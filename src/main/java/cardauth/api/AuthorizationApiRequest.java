package cardauth.api;

import cardauth.model.AuthorizationRequest;
import cardauth.model.Money;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Amount sign is deliberately not validated here: the domain declines it as INVALID_AMOUNT. */
public record AuthorizationApiRequest(
        @NotBlank String requestId,
        @NotBlank String cardId,
        @NotNull Long amountCents,
        @NotBlank String currency,
        String merchantName) {

    public AuthorizationRequest toDomain() {
        return new AuthorizationRequest(requestId, cardId, Money.of(currency, amountCents), merchantName);
    }
}
