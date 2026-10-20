package com.cadonuno.veracodecustomrolesui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class VeracodeCustomRolesUIApplication extends Application {
  private static Scene scene;

  public static void main(String[] args) {
    launch();
  }

  public static void runWithWaitCursor(Runnable toRun) {
    runWithWaitCursor(() -> {
      toRun.run();
      return null;
    }, null);
  }

  public static <T> void runWithWaitCursor(Supplier<T> toRun, Consumer<T> uiCallback) {
    if (scene == null) {
      T value = toRun.get();
      if (uiCallback != null) {
        Platform.runLater(() -> uiCallback.accept(value));
      }
      return;
    }

    Platform.runLater(() -> {
      Cursor currentCursor = scene.getCursor();
      scene.setCursor(Cursor.WAIT);
      scene.getRoot().setDisable(true);

      Thread operationThread = new Thread(() -> {
        T value = null;
        Throwable error = null;
        try {
          value = toRun.get();
        } catch (Throwable e) {
          error = e;
        } finally {
          final T finalValue = value;
          final Throwable finalError = error;
          try {
            Platform.runLater(() -> {
              try {
                restoreUIState(currentCursor);
                if (finalError != null) {
                  MessageHandler.showError("Error: " + getErrorMessage(finalError));
                } else if (uiCallback != null) {
                  try {
                    uiCallback.accept(finalValue);
                  } catch (Exception callbackError) {
                    MessageHandler.showError("Error: " + getErrorMessage(callbackError));
                  }
                }
              } catch (Exception restoreError) {
                restoreUIState(currentCursor);
              }
            });
          } catch (Exception platformError) {
            restoreUIState(currentCursor);
          }
        }
      });
      operationThread.setDaemon(true);
      operationThread.start();
    });
  }

  private static void restoreUIState(Cursor cursor) {
    if (scene != null) {
      scene.setCursor(cursor);
      scene.getRoot().setDisable(false);
    }
  }

  private static String getErrorMessage(Throwable throwable) {
    return throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getSimpleName();
  }

  @Override
  public void start(Stage stage) throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(VeracodeCustomRolesUIApplication.class.getResource("VeracodeCustomRolesUI-view.fxml"));
    Scene newScene = new Scene(fxmlLoader.load(), 320, 240);
    stage.setTitle("Veracode Custom Roles");
    stage.setScene(newScene);
    stage.show();
    stage.setMaximized(true);
    scene = newScene;
  }
}