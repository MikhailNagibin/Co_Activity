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

@Service
public class TokenService {

  private static final int TOKEN_EXPIRATION_MINUTES = 30;
  private final Map<Integer, String> activeTokens = new ConcurrentHashMap<>();
  private final Map<String, PendingVerification> pendingVerifications = new ConcurrentHashMap<>();

  public String createToken(Integer userId) {
    Instant expiresAt = Instant.now().plusSeconds(TOKEN_EXPIRATION_MINUTES * 60L);
    String payload = userId + ":" + expiresAt.toEpochMilli();
    return Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
  }

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

  public void registerToken(Integer userId, String token) {
    activeTokens.put(userId, token);
  }

  public void addPendingVerification(String login, PendingVerification pendingVerification) {
    pendingVerifications.put(login, pendingVerification);
  }

  public Optional<PendingVerification> getPendingVerification(String login) {
    return Optional.ofNullable(pendingVerifications.get(login));
  }

  public void removePendingVerification(String login) {
    pendingVerifications.remove(login);
  }

  public boolean isTokenActive(String token) {
    try {
      TokenPayload payload = decodeToken(token);
      if (payload.expiresAt().isBefore(Instant.now())) {
        return false;
      }
      String storedToken = activeTokens.get(payload.userId());
      return storedToken != null && storedToken.equals(token);
    } catch (TokenValidationException e) {
      return false;
    }
  }

  public void invalidateToken(String token) {
    try {
      TokenPayload payload = decodeToken(token);
      activeTokens.remove(payload.userId(), token);
    } catch (TokenValidationException e) {
      // Token was invalid anyway - no action needed
    }
  }
}
