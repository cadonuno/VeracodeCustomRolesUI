package com.cadonuno.veracodecustomrolesui.ui.components;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.cell.CheckBoxTreeCell;

import java.util.Optional;


public class CheckBoxTreeCellExt<T> extends CheckBoxTreeCell<T> implements ChangeListener<Boolean> {
  protected SimpleBooleanProperty linkedDisabledProperty;

  @Override
  public void updateItem(T item, boolean empty) {
    super.updateItem(item, empty);

    Optional.ofNullable(item)
        .map(nonNullItem -> treeItemProperty().getValue())
        .filter(treeItem -> treeItem instanceof CheckBoxTreeItemExt<?>)
        .map(treeItem -> (CheckBoxTreeItemExt<T>) treeItem)
        .ifPresent(checkItem -> {
          if (linkedDisabledProperty != null) {
            linkedDisabledProperty.removeListener(this);
          }

          linkedDisabledProperty = checkItem.enabledProperty;
          linkedDisabledProperty.addListener(this);

          setDisable(linkedDisabledProperty.get());
        });
  }

  @Override
  public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldVal, Boolean newVal) {
    setDisable(newVal);
  }
}