package com.coactivity.controller.dto.request;

import com.coactivity.controller.ApiController;
import com.coactivity.controller.dto.response.QrCodeResponse;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for generating a one-time QR code for participant verification.
 * <p>
 * Used when a room participant needs to create a time-sensitive verification code for physical
 * meeting access control. The generated QR code will be valid for exactly 1 minute and can only be
 * used once to prevent replay attacks.
 * </p>
 *
 * @see QrCodeResponse
 * @see ApiController#generateQrCode(String, GenerateQrCodeRequest)
 */
@Data
public class GenerateQrCodeRequest {

  /**
   * The unique identifier of the room for which the QR code is being generated.
   * <p>
   * This must be a valid room ID where the requesting user is an active participant. The system
   * will verify participant status before generating the QR code.
   * </p>
   */
  @NotNull(message = "Room ID is required and cannot be null")
  private Long roomId;
}