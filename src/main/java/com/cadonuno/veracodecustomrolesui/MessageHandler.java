package com.cadonuno.veracodecustomrolesui;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class MessageHandler {
  private static final boolean IS_DEBUG = System.getProperty("java.class.path").contains("idea_rt.jar");

  public MessageHandler() {
  }

  public static void showError(Exception anException) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter("detailed_errors.txt"))) {
      writer.write(ExceptionUtils.getStackTrace(anException));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    Platform.runLater(() -> new Alert(Alert.AlertType.ERROR,
        IS_DEBUG ? anException.getMessage() + ":\n" + ExceptionUtils.getStackTrace(anException) : "Unexpected error")
        .show());
  }

  public static void showError(String message) {
    Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, message).show());

  }

  public static void showSuccess(String message) {
    Platform.runLater(() -> new Alert(Alert.AlertType.INFORMATION, message).show());
  }

  public static boolean showConfirmation(String message) {
    return new Alert(Alert.AlertType.CONFIRMATION, message)
        .showAndWait().map(response ->
            response.getButtonData() == ButtonBar.ButtonData.OK_DONE
        ).orElse(false);
  }
}
