package com.blueship581.hedwig.vendor.client;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES-256-ECB + PKCS7 crypto helper for the SiSensing API.
 *
 * <p>Key is a 32-byte ASCII string extracted via static analysis of the iOS ECO app (v3.8). Both
 * login request body and response {@code data} field use the same key.
 */
class SiSensingCrypto {

  // 32-byte ASCII key, de-obfuscated from __DATA,__bss at runtime in the iOS binary
  private static final SecretKeySpec SECRET_KEY =
      new SecretKeySpec("6499f222539c46fcea2bd25cf6b3f5a4".getBytes(StandardCharsets.UTF_8), "AES");

  private SiSensingCrypto() {}

  /** Encrypts {@code plaintext} → Base64-encoded AES-256-ECB ciphertext. */
  static String encrypt(String plaintext) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
    cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY);
    byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(encrypted);
  }

  /** Decrypts a Base64-encoded AES-256-ECB ciphertext → UTF-8 plaintext. */
  static String decrypt(String ciphertext) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
    cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY);
    byte[] decoded = Base64.getDecoder().decode(ciphertext);
    byte[] decrypted = cipher.doFinal(decoded);
    return new String(decrypted, StandardCharsets.UTF_8);
  }
}
