package com.coactivity.auth.adapter.redis;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for Redis key naming in the auth subsystem.
 *
 * <p>Why this abstraction exists:</p>
 * <ul>
 *   <li>To keep key format consistent across different Redis adapters.</li>
 *   <li>To centralize key prefixes and make future changes explicit and auditable.</li>
 *   <li>To avoid storing PII (such as raw email) in Redis keys.</li>
 * </ul>
 *
 * <p>Keys are derived from {@code emailNormalized} using HMAC-SHA256 with a secret. This prevents
 * offline dictionary attacks where an attacker could guess emails and compute key suffixes without
 * knowing the secret.</p>
 */
@Component
public class AuthRedisKeys {

    private static final String REGISTRATION_CHALLENGE_PREFIX = "auth:register:";
    private static final String REGISTRATION_RESEND_COOLDOWN_PREFIX = REGISTRATION_CHALLENGE_PREFIX
            + "resend:";

    private static final String PASSWORD_RESET_CHALLENGE_PREFIX = "auth:password-reset:";
    private static final String PASSWORD_RESET_COOLDOWN_PREFIX = PASSWORD_RESET_CHALLENGE_PREFIX
            + "cooldown:";

    private final byte[] hmacSecret;

    public AuthRedisKeys(@Value("${app.auth.redis.key-secret:}") String keySecret) {
        if (keySecret == null || keySecret.isBlank()) {
            throw new IllegalStateException("app.auth.redis.key-secret must be set");
        }
        this.hmacSecret = keySecret.getBytes(StandardCharsets.UTF_8);
    }

    public String registrationChallengeKey(String emailNormalized) {
        validateEmailNormalized(emailNormalized);
        return REGISTRATION_CHALLENGE_PREFIX + hmacEmail(emailNormalized);
    }

    public String registrationResendCooldownKey(String emailNormalized) {
        validateEmailNormalized(emailNormalized);
        return REGISTRATION_RESEND_COOLDOWN_PREFIX + hmacEmail(emailNormalized);
    }

    public String passwordResetChallengeKey(String emailNormalized) {
        validateEmailNormalized(emailNormalized);
        return PASSWORD_RESET_CHALLENGE_PREFIX + hmacEmail(emailNormalized);
    }

    public String passwordResetCooldownKey(String emailNormalized) {
        validateEmailNormalized(emailNormalized);
        return PASSWORD_RESET_COOLDOWN_PREFIX + hmacEmail(emailNormalized);
    }

    private void validateEmailNormalized(String emailNormalized) {
        if (emailNormalized == null || emailNormalized.isBlank()) {
            throw new IllegalArgumentException("emailNormalized is required");
        }
    }

    /**
     * Deterministic (stable) HMAC used to remove PII from Redis keys.
     *
     * <p>Compared to a plain {@code SHA-256(email)}, HMAC protects against offline dictionary
     * attacks without knowledge of the secret: even if an attacker guesses the email value, they
     * cannot derive the correct key suffix without the secret.</p>
     */
    private String hmacEmail(String emailNormalized) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret, "HmacSHA256"));
            byte[] hash = mac.doFinal(emailNormalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256", ex);
        }
    }
}
