package com.coactivity;

import java.time.Instant;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

public class AuthToken {
  private static final Integer SECRET_KEY = 321;

  private AuthToken() {}

  public static String generateToken(String login, Integer id) {
    try {
      Instant finishAt = Instant.now().plusSeconds(30 * 60); // перевод из минут в секунды
      String payload = String.format("%s:%d:%d", login, id, finishAt.toEpochMilli());
      return encrypt(payload);
    } catch (Exception e) {
      throw new RuntimeException("Token generation failed", e);
    }
  }

  public static Integer getId(String token) {
    try {
      TokenData tokenData = decryptToken(token);
      return tokenData.id;
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid token", e);
    }
  }

  public static String getLogin(String token) {
    try {
      TokenData tokenData = decryptToken(token);
      return tokenData.login;
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid token", e);
    }
  }

  public static boolean isTokenExpired(String token) {
    try {
      TokenData tokenData = decryptToken(token);
      return Instant.now().isAfter(tokenData.finishAt);
    } catch (Exception e) {
      return true;
    }
  }

  private static class TokenData {
    String login;
    Integer id;
    Instant finishAt;

    TokenData(String login, Integer id, Instant finishAt) {
      this.login = login;
      this.id = id;
      this.finishAt = finishAt;
    }
  }

  private static TokenData decryptToken(String token) throws Exception {
    String decrypted = decrypt(token);
    StringTokenizer st = new StringTokenizer(decrypted, ":");

    if (st.countTokens() != 3) {
      throw new IllegalArgumentException("Invalid token format");
    }

    String login = st.nextToken();
    int id = Integer.parseInt(st.nextToken());
    long finishAtMillis = Long.parseLong(st.nextToken());
    Instant finishAt = Instant.ofEpochMilli(finishAtMillis);

    return new TokenData(login, id, finishAt);
  }

  private static String encrypt(String data) throws Exception {
    SecretKeySpec keySpec = generateKey();
    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
    cipher.init(Cipher.ENCRYPT_MODE, keySpec);
    byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
    return Base64.getUrlEncoder().encodeToString(encrypted);
  }

  private static String decrypt(String encryptedData) throws Exception {
    SecretKeySpec keySpec = generateKey();
    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
    cipher.init(Cipher.DECRYPT_MODE, keySpec);
    byte[] decoded = Base64.getUrlDecoder().decode(encryptedData);
    byte[] decrypted = cipher.doFinal(decoded);
    return new String(decrypted, StandardCharsets.UTF_8);
  }

  private static SecretKeySpec generateKey() {
    byte[] key = new byte[16];
    byte[] secretBytes = String.valueOf(SECRET_KEY).getBytes(StandardCharsets.UTF_8);
    System.arraycopy(secretBytes, 0, key, 0, Math.min(secretBytes.length, 16));
    return new SecretKeySpec(key, "AES");
  }
}