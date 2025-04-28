package com.cadonuno.veracodecustomrolesui.api;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class ApiResults {
  private final String errorMessage;
  private final JSONObject apiResponse;

  private ApiResults(JSONObject apiResponse) {
    this.apiResponse = apiResponse;
    this.errorMessage = null;
  }

  private ApiResults(String errorMessage) {
    this.apiResponse = null;
    this.errorMessage = errorMessage;
  }

  public static Optional<ApiResults> tryGetResults(HttpsURLConnection connection) {
    try {
      if (connection != null) {
        int responseCode = connection.getResponseCode();
        String errorMessage;
        JSONObject response;
        if (responseCode < 200 || responseCode > 299) {
          errorMessage = "Error Code: " + responseCode;
          try (InputStream responseInputStream = connection.getErrorStream()) {
            response = readResponse(responseInputStream);
            if (response != null) {
              errorMessage += "\nResponse Body: \n" + VeracodeApi.tryGetElementAsString(response, "message").orElse("EMPTY");
            }
            return Optional.of(new ApiResults(errorMessage));
          }
        }
        try (InputStream responseInputStream = connection.getInputStream()) {
          return Optional.of(new ApiResults(readResponse(responseInputStream)));
        }
      }
    } catch (IOException ignored) {

    }
    return Optional.empty();
  }

  private static JSONObject readResponse(InputStream responseInputStream) throws IOException, JSONException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    byte[] responseBytes = new byte[16384];

    int readResult;
    while ((readResult = responseInputStream.read(responseBytes, 0, responseBytes.length)) != -1) {
      outputStream.write(responseBytes, 0, readResult);
    }

    outputStream.flush();
    return outputStream.size() == 0 ? null : new JSONObject(outputStream.toString());
  }

  public JSONObject getApiResponse() {
    return this.apiResponse;
  }

  public boolean hasResponse() {
    return this.apiResponse != null;
  }

  public boolean didSucceed() {
    return apiResponse != null;
  }

  public String getErrorMessage() {
    return this.errorMessage;
  }
}
