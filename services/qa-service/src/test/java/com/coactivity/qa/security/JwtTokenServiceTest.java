package com.coactivity.qa.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenServiceTest {

  private static final String SECRET_BASE64 =
      "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=";

  @Test
  void resolveAuthorizedUserIdAcceptsAudienceClaimProducedByCoreService() {
    JwtTokenService jwtTokenService = new JwtTokenService();
    ReflectionTestUtils.setField(jwtTokenService, "secretBase64", SECRET_BASE64);
    ReflectionTestUtils.setField(jwtTokenService, "issuer", "coactivity-core");
    ReflectionTestUtils.setField(jwtTokenService, "audience", "coactivity-api");

    SecretKey signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_BASE64));
    Instant issuedAt = Instant.now();
    String token = Jwts.builder()
        .subject("42")
        .issuer("coactivity-core")
        .issuedAt(Date.from(issuedAt))
        .expiration(Date.from(issuedAt.plusSeconds(1800)))
        .claim("aud", "coactivity-api")
        .claim("roles", List.of())
        .signWith(signingKey)
        .compact();

    assertEquals(42, jwtTokenService.resolveAuthorizedUserId("Bearer " + token));
  }
}
