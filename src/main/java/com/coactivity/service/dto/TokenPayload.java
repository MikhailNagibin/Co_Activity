package com.coactivity.service.dto;

import java.time.Instant;

/**
 * Represents user information extracted from an authentication token.
 *
 * @param userId the user's unique identifier
 * @param expiresAt the token expiration timestamp
 */
public record TokenPayload(Integer userId, Instant expiresAt) {}