package com.coactivity.auth.adapter.redis;

import com.coactivity.auth.port.VerificationResult;
import com.coactivity.auth.adapter.redis.contract.InvalidAttemptOutcome;

/**
 * Maps Redis-internal outcomes of recording an invalid attempt to a port-level
 * {@link VerificationResult}.
 *
 * <p>This mapper exists to keep {@code RedisAttemptLimitedChallengeSupport} independent from
 * application/port types while still allowing adapter code to expose consistent results to the
 * application layer.</p>
 */
public final class VerificationResultMapper {

    private VerificationResultMapper() {
    }

    public static VerificationResult fromInvalidAttemptOutcome(InvalidAttemptOutcome outcome) {
        return switch (outcome) {
            case INVALID_CODE -> VerificationResult.INVALID_CODE;
            case TOO_MANY_ATTEMPTS -> VerificationResult.TOO_MANY_ATTEMPTS;
            case EXPIRED_OR_MISSING -> VerificationResult.EXPIRED_OR_MISSING;
        };
    }
}
