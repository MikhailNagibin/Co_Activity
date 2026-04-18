package com.coactivity.auth.adapter.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AuthRedisKeysTest {

  private final AuthRedisKeys keys = new AuthRedisKeys("test-secret");

  @Test
  void keys_matchExistingFormat() {
    String email = "user@example.com";
    String emailHash = "01d54a297ba437dea0ea85db3e939dff2f8947abd7925d12d1c46ae3ac4308a4";

    assertEquals("auth:register:" + emailHash, keys.registrationChallengeKey(email));
    assertEquals("auth:register:resend:" + emailHash, keys.registrationResendCooldownKey(email));

    assertEquals("auth:password-reset:" + emailHash, keys.passwordResetChallengeKey(email));
    assertEquals("auth:password-reset:cooldown:" + emailHash, keys.passwordResetCooldownKey(email));
  }

  @Test
  void keys_requireNonBlankEmailNormalized() {
    assertThrows(IllegalArgumentException.class, () -> keys.registrationChallengeKey(null));
    assertThrows(IllegalArgumentException.class, () -> keys.registrationChallengeKey(""));
    assertThrows(IllegalArgumentException.class, () -> keys.registrationChallengeKey("   "));
  }
}
