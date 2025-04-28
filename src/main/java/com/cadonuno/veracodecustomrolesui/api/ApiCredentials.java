package com.cadonuno.veracodecustomrolesui.api;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public final class ApiCredentials {
  private final String apiId;
  private final String apiKey;
  private final String instance;

  private ApiCredentials(String apiId, String apiKey, String instance) {
    this.apiId = apiId;
    this.apiKey = apiKey;
    this.instance = instance;
  }

  public static Optional<ApiCredentials> fromProfile(String profileName) {
    String userHome = System.getProperty("user.home");
    File credentialsFile = new File(new File(userHome, ".veracode"), "credentials");
    if (!credentialsFile.exists()) {
      throw new RuntimeException("Credentials file not found at " +
          credentialsFile.getAbsolutePath());
    }
    try (FileReader fileReader = new FileReader(credentialsFile);
         BufferedReader bufferedReader = new BufferedReader(fileReader)) {
      return getCredentialsFromFile(bufferedReader, profileName);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Optional<ApiCredentials> getCredentialsFromFile(BufferedReader bufferedReader,
                                                                 String profileName) throws IOException {
    String apiId = null;
    String apiKey = null;
    boolean foundProfile = false;
    for (String currentLine; (currentLine = bufferedReader.readLine()) != null; ) {
      currentLine = currentLine.trim();
      if (!currentLine.isEmpty()) {
        if (!foundProfile && currentLine.startsWith(
            wrapProfileName(profileName))) {
          foundProfile = true;
        } else if (foundProfile) {
          if (currentLine.charAt(0) == '[') {
            break;
          } else if (currentLine.startsWith("veracode_api_key_id")) {
            apiId = getCredentialValue(currentLine);
          } else if (currentLine.startsWith("veracode_api_key_secret")) {
            apiKey = getCredentialValue(currentLine);
          }
        }
      }
    }
    if (apiId == null || apiKey == null) {
      return Optional.empty();
    }
    return fromIdAndKey(apiId, apiKey);
  }

  public static Optional<ApiCredentials> fromIdAndKey(String apiId, String apiKey) {
    if (apiId == null || apiKey == null) {
      return Optional.empty();
    }
    apiId = apiId.trim();
    apiKey = apiKey.trim();
    if (apiId.isEmpty() || apiKey.isEmpty()) {
      return Optional.empty();
    }

    String finalApiId = apiId;
    String finalApiKey = apiKey;
    return getInstanceForApiId(apiId)
        .map((String instance) -> new ApiCredentials(finalApiId, finalApiKey, instance));
  }

  private static Optional<String> getInstanceForApiId(String apiId) {
    if (apiId.contains("-")) {
      String prefix = apiId.split("-")[0];
      if (prefix.length() != 8) {
        return Optional.empty();
      }
      char regionCharacter = prefix.charAt(6);
      if (regionCharacter == 'e') {
        return Optional.of("eu");
      } else if (regionCharacter == 'f') {
        return Optional.of("us");
      }
    }
    return Optional.of("com");
  }

  private static String getCredentialValue(String value) {
    return value == null ? null : StringUtils.substringAfter(value, "=").trim();
  }

  private static String wrapProfileName(String profileName) {
    return "[" + profileName + "]";
  }

  public String getApiId() {
    return apiId;
  }

  public String getApiKey() {
    return apiKey;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    ApiCredentials that = (ApiCredentials) obj;
    return Objects.equals(this.apiId, that.apiId) &&
        Objects.equals(this.apiKey, that.apiKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiId, apiKey);
  }

  @Override
  public String toString() {
    return "ApiCredentials[" +
        "apiId=" + apiId + ", " +
        "apiKey=" + apiKey + ']';
  }

  public String getInstance() {
    return this.instance;
  }
}
