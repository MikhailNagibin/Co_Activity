package com.coactivity.service;

import com.coactivity.service.dto.PendingVerification;
import com.coactivity.service.dto.TokenPayload;
import com.coactivity.service.exception.TokenValidationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Token service based on signed JWT access tokens.
 *
 * <p>For the training-stage architecture we intentionally keep only access tokens (no refresh
 * flow).
 * This means token revocation is best-effort via in-memory revoked jti set.
 */
@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private static final int DEFAULT_TOKEN_EXPIRATION_MINUTES = 30;
    private static final String DEFAULT_ISSUER = "coactivity-core";
    private static final String DEFAULT_AUDIENCE = "coactivity-api";
    private static final String DEFAULT_SECRET_BASE64 =
            "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=";
    /**
     * Legacy compatibility map used by tests that still seed pending verifications via
     * TokenService.
     */
    private final Map<String, PendingVerification> pendingVerifications = new ConcurrentHashMap<>();
    /**
     * In-memory revoked token ids (jti). Keeps logout behavior in a single instance deployment.
     */
    private final Set<String> revokedTokenIds = ConcurrentHashMap.newKeySet();
    @Value("${security.jwt.secret-base64:" + DEFAULT_SECRET_BASE64 + "}")
    private String jwtSecretBase64 = DEFAULT_SECRET_BASE64;
    @Value("${security.jwt.issuer:" + DEFAULT_ISSUER + "}")
    private String jwtIssuer = DEFAULT_ISSUER;
    @Value("${security.jwt.audience:" + DEFAULT_AUDIENCE + "}")
    private String jwtAudience = DEFAULT_AUDIENCE;
    @Value("${security.jwt.expiration-minutes:" + DEFAULT_TOKEN_EXPIRATION_MINUTES + "}")
    private int tokenExpirationMinutes = DEFAULT_TOKEN_EXPIRATION_MINUTES;
    private volatile SecretKey signingKey;

    public String createToken(Integer userId) {
        if (userId == null || userId <= 0) {
            throw new TokenValidationException("User id must be positive");
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(tokenExpirationMinutes * 60L);
        String tokenId = UUID.randomUUID().toString();

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(jwtIssuer)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .id(tokenId)
                .claim("aud", jwtAudience)
                .claim("roles", List.of())
                .signWith(resolveSigningKey())
                .compact();
    }

    public TokenPayload decodeToken(String token) {
        Claims claims = parseSignedClaims(token);
        try {
            Integer userId = Integer.parseInt(claims.getSubject());
            Instant expiresAt = claims.getExpiration().toInstant();
            return new TokenPayload(userId, expiresAt);
        } catch (Exception e) {
            throw new TokenValidationException("Invalid token subject", e);
        }
    }

    /**
     * Kept for backward compatibility. Access JWT is stateless and does not require registration.
     */
    public void registerToken(Integer userId, String token) {
        // No-op by design for stateless JWT.
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
            Claims claims = parseSignedClaims(token);
            if (!jwtIssuer.equals(claims.getIssuer())) {
                return false;
            }

            String audience = claims.get("aud", String.class);
            if (audience == null || !jwtAudience.equals(audience)) {
                return false;
            }

            String tokenId = claims.getId();
            return tokenId == null || !revokedTokenIds.contains(tokenId);
        } catch (TokenValidationException e) {
            return false;
        }
    }

    public void invalidateToken(String token) {
        try {
            Claims claims = parseSignedClaims(token);
            String tokenId = claims.getId();
            if (tokenId != null && !tokenId.isBlank()) {
                revokedTokenIds.add(tokenId);
            }
        } catch (TokenValidationException e) {
            // Token is already invalid; nothing else to do.
        }
    }

    private Claims parseSignedClaims(String rawToken) {
        String token = extractToken(rawToken);
        if (token == null || token.isBlank()) {
            throw new TokenValidationException("Token is required");
        }

        try {
            return Jwts.parser()
                    .verifyWith(resolveSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
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

            try {
                byte[] keyBytes = Decoders.BASE64.decode(jwtSecretBase64);
                signingKey = Keys.hmacShaKeyFor(keyBytes);

                if (DEFAULT_SECRET_BASE64.equals(jwtSecretBase64)) {
                    log.warn(
                            "Using default JWT secret. Set security.jwt.secret-base64 in env for non-local runs.");
                }
                return signingKey;
            } catch (Exception e) {
                throw new TokenValidationException("Invalid JWT secret configuration", e);
            }
        }
    }

    private String extractToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }
        return rawToken.startsWith("Bearer ") ? rawToken.substring(7) : rawToken;
    }
}
