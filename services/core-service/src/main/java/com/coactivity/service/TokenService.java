package com.coactivity.service;

import com.coactivity.service.dto.TokenPayload;
import com.coactivity.service.exception.TokenValidationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
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

    private static final int DEFAULT_TOKEN_EXPIRATION_MINUTES = 30;
    private static final String DEFAULT_ISSUER = "coactivity-core";
    private static final String DEFAULT_AUDIENCE = "coactivity-api";
    /**
     * In-memory revoked token ids (jti). Keeps logout behavior in a single instance deployment.
     */
    private final Set<String> revokedTokenIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
    @Value("${security.jwt.secret-base64:}")
    private String jwtSecretBase64 = "";
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

    public boolean isTokenActive(String token) {
        try {
            Claims claims = parseSignedClaims(token);
            if (!jwtIssuer.equals(claims.getIssuer())) {
                return false;
            }

            if (!audienceMatches(claims.get("aud"))) {
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

    private boolean audienceMatches(Object rawAudience) {
        if (rawAudience == null) {
            return false;
        }
        if (rawAudience instanceof String audience) {
            return jwtAudience.equals(audience);
        }
        if (rawAudience instanceof Collection<?> audiences) {
            return audiences.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .anyMatch(jwtAudience::equals);
        }
        return false;
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
                if (jwtSecretBase64 == null || jwtSecretBase64.isBlank()) {
                    throw new TokenValidationException("JWT secret is not configured");
                }
                byte[] keyBytes = Decoders.BASE64.decode(jwtSecretBase64);
                signingKey = Keys.hmacShaKeyFor(keyBytes);
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
