package com.coactivity.service;

import com.coactivity.service.dto.TokenPayload;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages authentication token lifecycle for the CoActivity platform.
 * <p>
 * Tokens are simple base64-encoded strings containing login and password credentials. Active tokens
 * are tracked in memory to prevent use of invalidated tokens.
 * </p>
 */
public class TokenService {

  private final Map<String, String> activeTokens = new ConcurrentHashMap<>();

  /**
   * Creates a new authentication token from user credentials.
   *
   * @param login    the user's email/login identifier
   * @param password the user's password for verification
   * @return base64-encoded token containing login:password
   */
  public String createToken(String login, String password) {
    String payload = login + ":" + password;
    return Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Decodes a token and extracts the user credentials.
   *
   * @param token the authentication token to decode
   * @return TokenPayload containing login and password
   * @throws IllegalArgumentException if token format is invalid
   */
  public TokenPayload decodeToken(String token) {
    try {
      String payload = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
      String[] parts = payload.split(":", 2);
      if (parts.length != 2) {
        throw new IllegalArgumentException("Invalid token format");
      }
      return new TokenPayload(parts[0], parts[1]);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid token", e);
    }
  }

  /**
   * Registers a token as active for the specified user.
   * <p>
   * If the user already has an active token, it will be replaced.
   * </p>
   *
   * @param login the user login to associate with the token
   * @param token the token to register as active
   */
  public void registerToken(String login, String token) {
    activeTokens.put(login, token);
  }

  /**
   * Verifies that a token is currently active and valid.
   *
   * @param token the token to validate
   * @return true if the token is registered and matches the stored active token
   */
  public boolean isTokenActive(String token) {
    try {
      TokenPayload payload = decodeToken(token);
      return token.equals(activeTokens.get(payload.login()));
    } catch (Exception e) {
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
      activeTokens.remove(payload.login(), token);
    } catch (Exception e) {
      // Token was invalid anyway - no action needed
    }
  }
}
