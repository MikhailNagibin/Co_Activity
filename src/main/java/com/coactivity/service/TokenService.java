package com.coactivity.service;

import com.coactivity.service.dto.TokenPayload;
import com.coactivity.service.exception.TokenValidationException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import com.coactivity.service.dto.PendingVerification;
import org.springframework.stereotype.Service;

/**
 * Manages authentication token lifecycle for the CoActivity platform.
 * <p>
 * Tokens are base64-encoded strings containing user id and expiration timestamp.
 * Tokens automatically expire 30 minutes after creation. Active tokens are tracked
 * in memory to prevent use of invalidated or expired tokens. This service is a
 * singleton managed by Spring, ensuring all services share the same active token registry.
 * </p>
 */
@Service
public class TokenService {

  /**
   * Token expiration duration in minutes.
   */
  private static final int TOKEN_EXPIRATION_MINUTES = 30;

  /**
   * Map of active tokens consisting of pairs (userId : token).
   * TokenService is singleton, so this field is shared across all service instances.
   */
  private final Map<Integer, String> activeTokens = new ConcurrentHashMap<>();

  /**
   * Map of pending verifications consisting of pairs (login : PendingVerification).
   */
  private final Map<String, PendingVerification> pendingVerifications = new ConcurrentHashMap<>();

  /**
   * Creates a new authentication token for the specified user.
   * <p>
   * The token automatically expires 30 minutes after creation. The token contains
   * the user's id and expiration timestamp encoded in base64 format.
   * </p>
   *
   * @param userId the user's unique identifier
   * @return base64-encoded token containing userId:expiresAt
   */
  public String createToken(Integer userId) {
    Instant expiresAt = Instant.now().plusSeconds(TOKEN_EXPIRATION_MINUTES * 60L);
    String payload = userId + ":" + expiresAt.toEpochMilli();
    return Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Decodes a token and extracts the user information.
   *
   * @param token the authentication token to decode
   * @return TokenPayload containing userId and expiresAt
   * @throws TokenValidationException if the token cannot be decoded
   */
  public TokenPayload decodeToken(String token) {
    try {
      String payload = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
      String[] parts = payload.split(":", 2);
      if (parts.length != 2) {
        throw new TokenValidationException("Invalid token format");
      }
      Integer userId = Integer.parseInt(parts[0]);
      Instant expiresAt = Instant.ofEpochMilli(Long.parseLong(parts[1]));
      return new TokenPayload(userId, expiresAt);
    } catch (TokenValidationException e) {
      throw e;
    } catch (Exception e) {
      throw new TokenValidationException("Invalid token", e);
    }
  }

  /**
   * Registers a token as active for the specified user.
   * <p>
   * If the user already has an active token, it will be replaced.
   * </p>
   *
   * @param userId the user id to associate with the token
   * @param token  the token to register as active
   */
  public void registerToken(Integer userId, String token) {
    activeTokens.put(userId, token);
  }

  /**
   * Adds a pending verification for a given login.
   *
   * @param login the login for which verification is pending
   * @param pendingVerification the pending verification object
   */
  public void addPendingVerification(String login, PendingVerification pendingVerification) {
    pendingVerifications.put(login, pendingVerification);
  }

  /**
   * Retrieves a pending verification for a given login.
   *
   * @param login the login for which verification is pending
   * @return an Optional containing the PendingVerification object if found, otherwise empty
   */
  public Optional<PendingVerification> getPendingVerification(String login) {
    return Optional.ofNullable(pendingVerifications.get(login));
  }

  /**
   * Removes a pending verification for a given login.
   *
   * @param login the login for which verification is to be removed
   */
  public void removePendingVerification(String login) {
    pendingVerifications.remove(login);
  }

  /**
   * Verifies that a token is currently active and valid.
   * <p>
   * A token is considered valid if:
   * <ul>
   *   <li>It can be successfully decoded</li>
   *   <li>It has not expired (expiresAt is in the future)</li>
   *   <li>It is registered and matches the stored active token for the user</li>
   * </ul>
   * </p>
   *
   * @param token the token to validate
   * @return true if the token is registered, not expired, and matches the stored active token
   */
  public boolean isTokenActive(String token) {
    try {
      TokenPayload payload = decodeToken(token);
      System.out.println();
      // Check if token has expired
      if (payload.expiresAt().isBefore(Instant.now())) {
        return false;
      }

      // Check if token is registered and matches the active token for this user
      return token.equals(activeTokens.get(payload.userId()));
    } catch (TokenValidationException e) {
      return false;
    }
  }

  /**
   * Invalidates a token, removing it from active tokens.
   * <p>
   * If the token doesn't match the current active token for the user, no action is taken.
   * </p>
   *
   * @param token the token to invalidate
   */
  public void invalidateToken(String token) {
    try {
      TokenPayload payload = decodeToken(token);
      activeTokens.remove(payload.userId(), token);
    } catch (TokenValidationException e) {
      // Token was invalid anyway - no action needed
    }
  }
}
