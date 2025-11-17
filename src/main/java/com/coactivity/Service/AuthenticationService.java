package com.coactivity.Service;

import com.coactivity.Service.dto.TokenPayload;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Handles creation and validation of the simple opaque tokens used by the controllers.
 * <p>
 * Tokens are lightweight wrappers around a user's login (email) and password. They are encrypted
 * with a server-side secret and encoded before being returned to the client. 
 * </p>
 */
public class AuthenticationService {

  /**
   * Hard-coded secret used to encrypt/decrypt the token payload.
   * <p>
   * Must be 16 bytes to work with AES-128. In production this should live in a secure config store.
   * </p>
   */
  private static final String SECRET_KEY = "CoActivitySecret!";

  private static final String ENCRYPTION_ALGORITHM = "AES/ECB/PKCS5Padding";

  /**
   * Creates an encoded token containing the provided login (email) and password.
   *
   * @param login    user email provided during authentication
   * @param password raw password; will be encrypted inside the token
   * @return opaque token string that clients must include in subsequent authenticated requests
   * @throws IllegalStateException if the encryption operation fails
   */
  public String createToken(String login, String password) {
    String payload = login + ":" + password;
    byte[] encryptedPayload = encrypt(payload);
    return Base64.getEncoder().encodeToString(encryptedPayload);
  }

  /**
   * Decrypts the provided token and extracts the login (email) and password.
   *
   * @param token encoded token previously created by {@link #createToken(String, String)}
   * @return {@link TokenPayload} containing the original login and password
   * @throws IllegalArgumentException if the token cannot be decoded or decrypted
   */
  public TokenPayload decodeToken(String token) {
    try {
      byte[] encryptedPayload = Base64.getDecoder().decode(token);
      String payload = decrypt(encryptedPayload);
      String[] parts = payload.split(":", 2);
      if (parts.length != 2) {
        throw new IllegalArgumentException("Token payload is malformed");
      }
      return new TokenPayload(parts[0], parts[1]);
    } catch (IllegalArgumentException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalArgumentException("Failed to decode token", ex);
    }
  }

  private byte[] encrypt(String payload) {
    try {
      Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
      SecretKeySpec secretKeySpec =
          new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
      cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
      return cipher.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to encrypt authentication token", ex);
    }
  }

  private String decrypt(byte[] encryptedPayload) {
    try {
      Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
      SecretKeySpec secretKeySpec =
          new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
      cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
      byte[] decrypted = cipher.doFinal(encryptedPayload);
      return new String(decrypted, StandardCharsets.UTF_8);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to decrypt authentication token", ex);
    }
  }

}
