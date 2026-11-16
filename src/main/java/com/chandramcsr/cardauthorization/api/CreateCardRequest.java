package com.chandramcsr.cardauthorization.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.YearMonth;

public record CreateCardRequest(
        @NotBlank String cardId,
        @NotNull YearMonth expiration,
        @NotNull @Positive Long creditLimitCents,
        @NotBlank String currency) {
}
