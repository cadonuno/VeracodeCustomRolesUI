package com.cadonuno.veracodecustomrolesui.ui.components;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.CheckBoxTreeItem;


public class CheckBoxTreeItemExt<T> extends CheckBoxTreeItem<T> {
  public SimpleBooleanProperty disabledProperty = new SimpleBooleanProperty(false);

  public CheckBoxTreeItemExt(T t) {
    super(t);
  }


  public boolean isEnabled() {
    return disabledProperty.get();
  }

  public void setDisabled(boolean disabled) {
    disabledProperty.set(disabled);
  }
}