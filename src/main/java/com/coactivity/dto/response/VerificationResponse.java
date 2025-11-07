package com.coactivity.dto.response;

import com.coactivity.controller.ApiController;
import com.coactivity.dto.request.VerifyQrCodeRequest;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for QR code verification results during physical check-in.
 * <p>
 * Provides comprehensive verification status including participant identity and room context. Used
 * by both scanning applications and administrative interfaces to confirm legitimate access to
 * physical meeting spaces.
 * </p>
 *
 * @see VerifyQrCodeRequest
 * @see ApiController#verifyQrCode(String, VerifyQrCodeRequest)
 */
@Data
@Builder
public class VerificationResponse {

  /**
   * Indicates whether the QR code verification was successful.
   * <p>
   * {@code true} if all validation checks pass (valid code, not expired, not previously used, user
   * is room participant). {@code false} if any verification step fails.
   * </p>
   */
  private boolean valid;

  /**
   * Human-readable message describing the verification outcome.
   * <p>
   * Provides context for both success and failure scenarios, suitable for direct display to end
   * users in UI interfaces.
   * </p>
   *
   * @example "Verification successful - John Doe confirmed for Basketball Club"
   * @example "QR code has expired - please generate a new code"
   */
  private String message;

  /**
   * Display name of the user who scanned the QR code.
   * <p>
   * Only populated on successful verification to confirm participant identity to meeting
   * organizers. Null when verification fails.
   * </p>
   */
  private String userName;

  /**
   * Official name of the room/activity being accessed.
   * <p>
   * Confirms which specific activity the user is verified for, useful in scenarios with multiple
   * simultaneous meetings.
   * </p>
   */
  private String roomName;

  /**
   * Unique identifier of the verified user.
   * <p>
   * Useful for audit trails and integration with other systems. Only populated on successful
   * verification.
   * </p>
   */
  private Long userId;

  /**
   * Unique identifier of the accessed room.
   * <p>
   * Provides machine-readable room context for further processing or integration with attendance
   * tracking systems.
   * </p>
   */
  private Long roomId;
}