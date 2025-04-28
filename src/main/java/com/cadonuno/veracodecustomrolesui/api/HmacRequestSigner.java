package com.cadonuno.veracodecustomrolesui.api;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;


public final class HmacRequestSigner {
  public static final int BYTE_ARRAY_SIZE = 16;
  private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
  // Included in the signature to inform Veracode of the signature version.
  private static final String VERACODE_REQUEST_VERSION_STRING = "vcode_request_version_1";
  // Expected format for the unencrypted data string.
  private static final String DATA_FORMAT = "id=%s&host=%s&url=%s&method=%s";
  // Expected format for the Authorization header.
  private static final String HEADER_FORMAT = "%s id=%s,ts=%s,nonce=%s,sig=%s";
  // Expect prefix to the Authorization header.
  private static final String VERACODE_HMAC_SHA_256 = "VERACODE-HMAC-SHA-256";
  // HMAC encryption algorithm.
  private static final String HMAC_SHA_256 = "HmacSHA256";
  // A cryptographically secure random number generator.
  private static final SecureRandom secureRandom = new SecureRandom();

  // Private constructor.
  private HmacRequestSigner() {
    /*
     * This is a utility class that should only be accessed through its
     * static methods.
     */
  }


  /**
   * Entry point for HmacRequestSigner. Returns the value for the
   * Authorization header for use with Veracode APIs when provided an API id,
   * secret key, and target URL.
   *
   * @param url The URL of the called API, including query parameters
   * @return The value to be put in the Authorization header
   */
  public static String getVeracodeAuthorizationHeader(final ApiCredentials apiCredentials,
                                                      final URL url, final String httpMethod)
      throws InvalidKeyException, NoSuchAlgorithmException, IllegalStateException, UnsupportedEncodingException {
    final String urlPath = (url.getQuery() == null)
        ? url.getPath()
        : url.getPath().concat("?").concat(url.getQuery());
    final String data = String.format(DATA_FORMAT, apiCredentials.getApiId(), url.getHost(), urlPath, httpMethod);
    final String timestamp = String.valueOf(System.currentTimeMillis());
    byte[] randomBytes = generateRandomBytes();
    final String nonce = bytesToHex(randomBytes).toLowerCase(Locale.US);
    final String signature = getSignature(apiCredentials.getApiKey(), data, timestamp, nonce);
    return String.format(HEADER_FORMAT, VERACODE_HMAC_SHA_256, apiCredentials.getApiId(), timestamp, nonce, signature);
  }

  /*
   * Generate the signature expected by the Veracode platform by chaining
   * encryption routines in the correct order.
   */
  private static String getSignature(final String key, final String data, final String timestamp, final String nonce)
      throws InvalidKeyException, NoSuchAlgorithmException, IllegalStateException {
    final byte[] keyBytes = decodeHexString(key);
    final byte[] nonceBytes = decodeHexString(nonce);
    final byte[] encryptedNonce = hmacSha256(nonceBytes, keyBytes);
    final byte[] encryptedTimestamp = hmacSha256(timestamp, encryptedNonce);
    final byte[] signingKey = hmacSha256(VERACODE_REQUEST_VERSION_STRING, encryptedTimestamp);
    final byte[] signature = hmacSha256(data, signingKey);
    return printHexBytesAsString(signature);
  }

  // Encrypt a string using the provided key.
  private static byte[] hmacSha256(final String data, final byte[] key)
      throws NoSuchAlgorithmException, InvalidKeyException, IllegalStateException {
    final Mac mac = Mac.getInstance(HMAC_SHA_256);
    mac.init(new SecretKeySpec(key, HMAC_SHA_256));
    return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
  }

  // Encrypt a byte array using the provided key.
  private static byte[] hmacSha256(final byte[] data, final byte[] key)
      throws NoSuchAlgorithmException, InvalidKeyException {
    final Mac mac = Mac.getInstance(HMAC_SHA_256);
    mac.init(new SecretKeySpec(key, HMAC_SHA_256));
    return mac.doFinal(data);
  }

  // Generate a random byte array for cryptographic use.
  private static byte[] generateRandomBytes() {
    final byte[] key = new byte[BYTE_ARRAY_SIZE];
    secureRandom.nextBytes(key);
    return key;
  }

  public static String bytesToHex(byte[] bytes) {
    byte[] hexChars = new byte[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEX_ARRAY[v >>> 4];
      hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
    }
    return new String(hexChars, StandardCharsets.UTF_8);
  }

  public static byte[] decodeHexString(String hexString) {
    if (hexString.length() % 2 == 1) {
      throw new IllegalArgumentException(
          "Invalid hexadecimal String supplied.");
    }

    byte[] bytes = new byte[hexString.length() / 2];
    for (int i = 0; i < hexString.length(); i += 2) {
      bytes[i / 2] = hexToByte(hexString.substring(i, i + 2));
    }
    return bytes;
  }

  public static byte hexToByte(String hexString) {
    int firstDigit = toDigit(hexString.charAt(0));
    int secondDigit = toDigit(hexString.charAt(1));
    return (byte) ((firstDigit << 4) + secondDigit);
  }

  private static int toDigit(char hexChar) {
    int digit = Character.digit(hexChar, 16);
    if (digit == -1) {
      throw new IllegalArgumentException(
          "Invalid Hexadecimal Character: " + hexChar);
    }
    return digit;
  }

  public static String printHexBytesAsString(byte[] bytes) {
    StringBuilder result = new StringBuilder();
    for (byte aByte : bytes) {
      result.append(String.format("%02x", aByte));
    }
    return result.toString();
  }
}