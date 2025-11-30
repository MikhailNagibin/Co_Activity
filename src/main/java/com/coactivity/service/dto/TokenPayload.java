package com.coactivity.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

/**
 * Represents user information extracted from an authentication token.
 *
 * @param userId    the user's unique identifier
 * @param expiresAt the token expiration timestamp
 */
public record TokenPayload(
    @NotNull
    @Positive
    Integer userId,
    @NotNull
    Instant expiresAt) {

}