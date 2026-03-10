package com.coactivity.qa.security;

import com.coactivity.qa.exception.TokenValidationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

  private static final String DEFAULT_SECRET_BASE64 =
      "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=";
  private static final String DEFAULT_ISSUER = "coactivity-core";
  private static final String DEFAULT_AUDIENCE = "coactivity-api";

  @Value("${security.jwt.secret-base64:" + DEFAULT_SECRET_BASE64 + "}")
  private String secretBase64 = DEFAULT_SECRET_BASE64;

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

      String tokenAudience = claims.get("aud", String.class);
      if (tokenAudience == null || !audience.equals(tokenAudience)) {
        throw new TokenValidationException("Token audience mismatch");
      }

      return claims;
    } catch (JwtException | IllegalArgumentException e) {
      throw new TokenValidationException("Invalid token", e);
    }
  }

  private SecretKey resolveSigningKey() {
    if (signingKey != null) {
      return signingKey;
    }

    synchronized (this) {
      if (signingKey != null) {
        return signingKey;
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
