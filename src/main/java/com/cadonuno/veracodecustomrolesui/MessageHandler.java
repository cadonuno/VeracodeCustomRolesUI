package com.cadonuno.veracodecustomrolesui;

import javafx.scene.control.Alert;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class MessageHandler {
  private static final boolean IS_DEBUG = System.getProperty("java.class.path").contains("idea_rt.jar");

  public MessageHandler() {
  }

  public static void showError(Exception anException) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setContentText(IS_DEBUG ? anException.getMessage() + ":\n" + ExceptionUtils.getStackTrace(anException) : "Unexpected error");
    alert.show();
  }

  public static void showError(String errorMessage) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setContentText(errorMessage);
    alert.show();
  }

  public static void showSuccess(String message) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setContentText(message);
    alert.show();
  }
}
