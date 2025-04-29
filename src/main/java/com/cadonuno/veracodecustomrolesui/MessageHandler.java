package com.cadonuno.veracodecustomrolesui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class MessageHandler {
  private static final boolean IS_DEBUG = System.getProperty("java.class.path").contains("idea_rt.jar");

  public MessageHandler() {
  }

  public static void showError(Exception anException) {
    new Alert(Alert.AlertType.ERROR,
        IS_DEBUG ? anException.getMessage() + ":\n" + ExceptionUtils.getStackTrace(anException) : "Unexpected error")
        .show();
  }

  public static void showError(String message) {
    new Alert(Alert.AlertType.ERROR, message).show();
  }

  public static void showSuccess(String message) {
    new Alert(Alert.AlertType.INFORMATION, message).show();
  }

  public static boolean showConfirmation(String message) {
    return new Alert(Alert.AlertType.CONFIRMATION, message)
        .showAndWait().map(response ->
            response.getButtonData() == ButtonBar.ButtonData.OK_DONE
        ).orElse(false);
  }
}
