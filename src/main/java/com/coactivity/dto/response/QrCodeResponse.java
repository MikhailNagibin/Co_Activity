package com.coactivity.dto.response;

import com.coactivity.controller.ApiController;
import com.coactivity.dto.request.GenerateQrCodeRequest;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO containing generated QR code data for participant verification.
 * <p>
 * Returned after successful QR code generation, providing the time-sensitive code that should be
 * displayed as a QR image for scanning. Frontend applications should encode the {@code qrUrl} into
 * a QR code image and display it with a countdown timer based on {@code expiresAt}.
 * </p>
 *
 * @author Your Name
 * @version 1.0
 * @see GenerateQrCodeRequest
 * @see ApiController#generateQrCode(String, GenerateQrCodeRequest)
 */
@Data
@Builder
public class QrCodeResponse {

  /**
   * The unique QR code string to be encoded in the QR image.
   * <p>
   * This cryptographic string serves as the one-time token for verification. It should be encoded
   * into a QR code format (typically using a frontend library like qrcode.js) and displayed at the
   * physical meeting location.
   * </p>
   */
  private String qrCode;

  /**
   * The complete URL that should be encoded in the QR code for scanning.
   * <p>
   * When scanned, mobile devices will navigate to this URL which should trigger the verification
   * process in the web application. The URL contains both the QR code and room context as query
   * parameters.
   * </p>
   *
   * @example "https://coactivity.com/verify-qr?code=AbCdEfG123&roomId=12345"
   */
  private String qrUrl;

  /**
   * The timestamp when this QR code becomes invalid and cannot be used.
   * <p>
   * QR codes have a strict 1-minute validity window from generation time to prevent unauthorized
   * access. Clients should display a countdown timer and automatically refresh the QR code after
   * expiration.
   * </p>
   */
  private Instant expiresAt;

  /**
   * The room identifier this QR code is valid for.
   * <p>
   * Provides context to the QR code generator about which room participants can be verified with
   * this code. Useful for audit trails and debugging.
   * </p>
   */
  private Long roomId;
}