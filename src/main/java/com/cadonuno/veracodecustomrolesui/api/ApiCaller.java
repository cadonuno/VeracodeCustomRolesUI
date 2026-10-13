package com.cadonuno.veracodecustomrolesui.api;

import com.cadonuno.veracodecustomrolesui.Logger;
import com.cadonuno.veracodecustomrolesui.MessageHandler;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class ApiCaller {
  private static final String URL_BASE = "https://api.veracode.";

  public static Optional<ApiResults> runApi(ApiCredentials apiCredentials, String apiUrl, String requestType, String jsonParameters) {

    HttpsURLConnection connection;
    String fullUrl = URL_BASE + apiCredentials.getInstance() + apiUrl;
    try {
      URL fullApiUrl = new URL(fullUrl);
      String authorizationHeader = HmacRequestSigner.getVeracodeAuthorizationHeader(
          apiCredentials,
          fullApiUrl, requestType);
      connection = (HttpsURLConnection) fullApiUrl.openConnection();
      connection.setRequestMethod(requestType);
      connection.setRequestProperty("Authorization", authorizationHeader);
      if (jsonParameters != null) {
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        try (OutputStream outputStream = connection.getOutputStream()) {
          byte[] input = jsonParameters.getBytes(StandardCharsets.UTF_8);
          outputStream.write(input, 0, input.length);
        }
      }

      return ApiResults.tryGetResults(connection);
    } catch (Exception e) {
      Logger.log("Unable to run API at: " + fullUrl + "\n\tWith parameters: " + jsonParameters);
      MessageHandler.showError(e);
      return Optional.of(ApiResults.makeErrorResults(ExceptionUtils.getStackTrace(e)));
    }
  }
}
