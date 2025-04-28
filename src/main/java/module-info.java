module com.cadonuno.veracodecustomrolesui {
  requires javafx.controls;
  requires javafx.fxml;

  requires org.kordamp.bootstrapfx.core;
  requires org.apache.sling.commons.json;
  requires org.apache.commons.lang3;
  requires java.desktop;

  opens com.cadonuno.veracodecustomrolesui to javafx.fxml;
  exports com.cadonuno.veracodecustomrolesui;
  exports com.cadonuno.veracodecustomrolesui.ui;
  opens com.cadonuno.veracodecustomrolesui.ui to javafx.fxml;
  opens com.cadonuno.veracodecustomrolesui.models to javafx.fxml;
  exports com.cadonuno.veracodecustomrolesui.models;
  exports com.cadonuno.veracodecustomrolesui.ui.components;
  opens com.cadonuno.veracodecustomrolesui.ui.components to javafx.fxml;
}