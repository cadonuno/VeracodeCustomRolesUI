package com.cadonuno.veracodecustomrolesui.ui.components;

import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.util.Callback;

public class CheckBoxTreeCellFactoryExt<T> implements Callback<TreeView<T>, TreeCell<T>> {
  @Override
  public TreeCell<T> call(TreeView<T> tv) {
    return new CheckBoxTreeCellExt<>();
  }
}