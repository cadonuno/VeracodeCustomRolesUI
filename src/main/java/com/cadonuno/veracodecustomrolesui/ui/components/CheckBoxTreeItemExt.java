package com.cadonuno.veracodecustomrolesui.ui.components;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.CheckBoxTreeItem;


public class CheckBoxTreeItemExt<T> extends CheckBoxTreeItem<T> {
  public SimpleBooleanProperty enabledProperty = new SimpleBooleanProperty(true);

  public CheckBoxTreeItemExt(T t) {
    super(t);
  }

  public boolean isEnabled() {
    return enabledProperty.get();
  }

  public void setDisabled(boolean disabled) {
    enabledProperty.set(disabled);
  }
}