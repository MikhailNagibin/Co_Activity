package com.coactivity.service.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import lombok.NonNull;

/**
 * Lightweight record representing a pending login verification state.
 */
public record PendingVerification(
    @NotNull
    @Positive
    Integer userId,
    @NotBlank
    @NonNull
    String code,
    @NotNull
    @Future
    Instant expiresAt) {

}
