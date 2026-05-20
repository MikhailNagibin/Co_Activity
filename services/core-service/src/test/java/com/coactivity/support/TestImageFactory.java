package com.coactivity.support;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public final class TestImageFactory {

  private TestImageFactory() {
  }

  public static byte[] png() {
    return writeBufferedImage("png");
  }

  public static byte[] jpeg() {
    return writeBufferedImage("jpeg");
  }

  public static byte[] webp() {
    return new byte[]{
        'R', 'I', 'F', 'F',
        0x00, 0x00, 0x00, 0x00,
        'W', 'E', 'B', 'P',
        'V', 'P', '8', ' '
    };
  }

  public static byte[] invalidImagePayload() {
    return "not-an-image".getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }

  private static byte[] writeBufferedImage(String format) {
    BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    image.setRGB(0, 0, 0x00AA55);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      boolean written = ImageIO.write(image, format, outputStream);
      if (!written) {
        throw new IllegalStateException("Unable to encode test image as " + format);
      }
      return outputStream.toByteArray();
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to create test image", ex);
    }
  }
}
