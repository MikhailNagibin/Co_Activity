package com.coactivity.util;

import java.nio.charset.StandardCharsets;

public final class ImageContentValidator {

  private static final byte[] PNG_SIGNATURE = new byte[]{
      (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'
  };
  private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] WEBP = "WEBP".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] VP8 = "VP8 ".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] VP8L = "VP8L".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] VP8X = "VP8X".getBytes(StandardCharsets.US_ASCII);

  private ImageContentValidator() {
  }

  public static boolean matchesContentType(String contentType, byte[] bytes) {
    if (contentType == null || bytes == null || bytes.length == 0) {
      return false;
    }
    return switch (contentType) {
      case "image/jpeg" -> isJpeg(bytes);
      case "image/png" -> isPng(bytes);
      case "image/webp" -> isWebp(bytes);
      default -> false;
    };
  }

  private static boolean isJpeg(byte[] bytes) {
    return bytes.length >= 4
        && bytes[0] == (byte) 0xFF
        && bytes[1] == (byte) 0xD8
        && bytes[2] == (byte) 0xFF
        && bytes[bytes.length - 2] == (byte) 0xFF
        && bytes[bytes.length - 1] == (byte) 0xD9;
  }

  private static boolean isPng(byte[] bytes) {
    if (bytes.length < PNG_SIGNATURE.length) {
      return false;
    }
    for (int i = 0; i < PNG_SIGNATURE.length; i++) {
      if (bytes[i] != PNG_SIGNATURE[i]) {
        return false;
      }
    }
    return true;
  }

  private static boolean isWebp(byte[] bytes) {
    return bytes.length >= 16
        && hasPrefix(bytes, 0, RIFF)
        && hasPrefix(bytes, 8, WEBP)
        && (hasPrefix(bytes, 12, VP8)
        || hasPrefix(bytes, 12, VP8L)
        || hasPrefix(bytes, 12, VP8X));
  }

  private static boolean hasPrefix(byte[] bytes, int offset, byte[] prefix) {
    if (bytes.length < offset + prefix.length) {
      return false;
    }
    for (int i = 0; i < prefix.length; i++) {
      if (bytes[offset + i] != prefix[i]) {
        return false;
      }
    }
    return true;
  }
}
