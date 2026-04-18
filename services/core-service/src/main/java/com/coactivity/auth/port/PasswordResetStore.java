package com.coactivity.auth.port;

/**
 * Port for password reset challenges and request cooldown.
 *
 * <p>This interface hides storage details from the application layer and allows alternative
 * implementations (e.g. Redis, database, in-memory fake for tests).</p>
 */
public interface PasswordResetStore {

    /**
     * Creates or overwrites the password reset challenge for the given user/email.
     */
    void create(String emailNormalized, Integer userId, String rawCode);

    /**
     * Verifies a password reset code without consuming (deleting) the challenge on success.
     */
    VerificationResult inspect(String emailNormalized, String code);

    /**
     * Verifies a password reset code and consumes the challenge on success.
     */
    VerificationResult consume(String emailNormalized, String code);

    /**
     * Atomically starts a cooldown window for password reset requests.
     *
     * @return {@code true} if the cooldown was started now; {@code false} if a cooldown is already
     * active.
     */
    boolean tryActivateRequestCooldown(String emailNormalized);

    /**
     * Clears the password reset request cooldown flag.
     */
    void clearRequestCooldown(String emailNormalized);

    /**
     * Deletes the active password reset challenge, if any.
     */
    void delete(String emailNormalized);
}
