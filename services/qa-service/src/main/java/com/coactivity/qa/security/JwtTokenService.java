package com.coactivity.qa.security;

import com.coactivity.qa.exception.TokenValidationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Collection;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

  private static final String DEFAULT_ISSUER = "coactivity-core";
  private static final String DEFAULT_AUDIENCE = "coactivity-api";

  @Value("${security.jwt.secret-base64:}")
  private String secretBase64 = "";

  @Value("${security.jwt.issuer:" + DEFAULT_ISSUER + "}")
  private String issuer = DEFAULT_ISSUER;

  @Value("${security.jwt.audience:" + DEFAULT_AUDIENCE + "}")
  private String audience = DEFAULT_AUDIENCE;

  private volatile SecretKey signingKey;

  public Integer resolveAuthorizedUserId(String authorizationHeader) {
    String token = extractBearer(authorizationHeader);
    Claims claims = parseClaims(token);

    String subject = claims.getSubject();
    if (subject == null || subject.isBlank()) {
      throw new TokenValidationException("Token subject is missing");
    }

    try {
      return Integer.parseInt(subject);
    } catch (NumberFormatException e) {
      throw new TokenValidationException("Token subject is invalid", e);
    }
  }

  private Claims parseClaims(String token) {
    try {
      Claims claims = Jwts.parser()
          .verifyWith(resolveSigningKey())
          .build()
          .parseSignedClaims(token)
          .getPayload();

      if (!issuer.equals(claims.getIssuer())) {
        throw new TokenValidationException("Token issuer mismatch");
      }

      if (!audienceMatches(claims.get("aud"))) {
        throw new TokenValidationException("Token audience mismatch");
      }

      return claims;
    } catch (JwtException | IllegalArgumentException e) {
      throw new TokenValidationException("Invalid token", e);
    }
  }

  private boolean audienceMatches(Object rawAudience) {
    if (rawAudience == null) {
      return false;
    }
    if (rawAudience instanceof String tokenAudience) {
      return audience.equals(tokenAudience);
    }
    if (rawAudience instanceof Collection<?> audiences) {
      return audiences.stream()
          .filter(String.class::isInstance)
          .map(String.class::cast)
          .anyMatch(audience::equals);
    }
    return false;
  }

  private SecretKey resolveSigningKey() {
    if (signingKey != null) {
      return signingKey;
    }

    synchronized (this) {
      if (signingKey != null) {
        return signingKey;
      }
      if (secretBase64 == null || secretBase64.isBlank()) {
        throw new TokenValidationException("JWT secret is not configured");
      }
      byte[] keyBytes = Decoders.BASE64.decode(secretBase64);
      signingKey = Keys.hmacShaKeyFor(keyBytes);
      return signingKey;
    }
  }

  private String extractBearer(String authorizationHeader) {
    if (authorizationHeader == null || authorizationHeader.isBlank()) {
      throw new TokenValidationException("Authorization token is required");
    }

    if (authorizationHeader.startsWith("Bearer ")) {
      return authorizationHeader.substring(7);
    }

    return authorizationHeader;
  }
}
