package com.coactivity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coactivity.service.exception.TokenValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TokenServiceTest {

  private static final String SECRET_BASE64 =
      "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=";

  @Test
  void createTokenProducesActiveTokenForProtectedEndpoints() {
    TokenService tokenService = new TokenService();
    ReflectionTestUtils.setField(tokenService, "jwtSecretBase64", SECRET_BASE64);
    ReflectionTestUtils.setField(tokenService, "jwtIssuer", "coactivity-core");
    ReflectionTestUtils.setField(tokenService, "jwtAudience", "coactivity-api");

    String token = tokenService.createToken(42);

    assertTrue(tokenService.isTokenActive(token));
    assertEquals(42, tokenService.decodeToken(token).userId());
  }

  @Test
  void createTokenFailsFastWhenSecretIsMissing() {
    TokenService tokenService = new TokenService();

    assertThrows(TokenValidationException.class, () -> tokenService.createToken(42));
  }
}
