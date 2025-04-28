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

    Thread operationThread = new Thread(() -> {
      Cursor currentCursor = scene.getCursor();
      scene.setCursor(Cursor.WAIT);
      scene.getRoot().setDisable(true);
      try {
        T value = toRun.get();
        if (uiCallback != null) {
          Platform.runLater(() -> uiCallback.accept(value));
        }
      } finally {
        scene.setCursor(currentCursor);
        scene.getRoot().setDisable(false);
      }
    });
    operationThread.setDaemon(true);
    operationThread.start();
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