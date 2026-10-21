package com.cadonuno.veracodecustomrolesui.api;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@SuppressWarnings("deprecation")
public class ApiResults {
  private final String errorMessage;
  private final JSONObject apiResponse;

  @SuppressWarnings("unused")
  public String getRawJson() {
    return rawJson;
  }

  private final String rawJson;

  private ApiResults(JSONObject apiResponse) {
    this.apiResponse = apiResponse;
    this.errorMessage = null;
    this.rawJson = apiResponse.toString();
  }

  private ApiResults(String errorMessage, String rawJson) {
    this.apiResponse = null;
    this.errorMessage = errorMessage;
    this.rawJson = rawJson;
  }

  public static Optional<ApiResults> tryGetResults(HttpsURLConnection connection) {
    try {
      if (connection != null) {
        int responseCode = connection.getResponseCode();
        String errorMessage;
        if (responseCode < 200 || responseCode > 299) {
          errorMessage = "Error Code: " + responseCode;
          try (InputStream responseInputStream = connection.getErrorStream()) {
            Optional<JSONObject> response;
            response = readResponse(responseInputStream);
            if (response.isPresent()) {
              errorMessage += "\nResponse Body: \n" + VeracodeApi.tryGetElementAsString(response.get(), "message").orElse("EMPTY");
            }
            return Optional.of(new ApiResults(errorMessage, response.map(JSONObject::toString).orElse("")));
          }
        }
        try (InputStream responseInputStream = connection.getInputStream()) {
          return readResponse(responseInputStream).map(ApiResults::new);
        }
      }
    } catch (IOException ignored) {

    }
    return Optional.empty();
  }

  private static Optional<JSONObject> readResponse(InputStream responseInputStream) throws IOException, JSONException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    byte[] responseBytes = new byte[16384];

    int readResult;
    while ((readResult = responseInputStream.read(responseBytes, 0, responseBytes.length)) != -1) {
      outputStream.write(responseBytes, 0, readResult);
    }

    outputStream.flush();
    return outputStream.size() == 0 ? Optional.empty() : Optional.of(new JSONObject(outputStream.toString()));
  }

  public static ApiResults makeErrorResults(String message) {
    return new ApiResults(message, "");
  }

  public JSONObject getApiResponse() {
    return this.apiResponse;
  }

  public boolean hasResponse() {
    return this.apiResponse != null;
  }

  public boolean didFail() {
    return errorMessage != null;
  }

  public String getErrorMessage() {
    return this.errorMessage;
  }
}
