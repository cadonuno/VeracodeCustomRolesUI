package com.cadonuno.veracodecustomrolesui.ui;

import com.cadonuno.veracodecustomrolesui.MessageHandler;
import com.cadonuno.veracodecustomrolesui.VeracodeCustomRolesUIApplication;
import com.cadonuno.veracodecustomrolesui.api.ApiCredentials;
import com.cadonuno.veracodecustomrolesui.api.VeracodeApi;
import com.cadonuno.veracodecustomrolesui.models.VeracodePermission;
import com.cadonuno.veracodecustomrolesui.models.VeracodeRole;
import com.cadonuno.veracodecustomrolesui.ui.components.CheckBoxTreeCellFactoryExt;
import com.cadonuno.veracodecustomrolesui.ui.components.CheckBoxTreeItemExt;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class VeracodeCustomRolesUIController {
  public static final String CUSTOM_ROLE_NAME_PREFIX = "custom-";
  public TextField credentialsProfile;
  public PasswordField apiIdField;
  public PasswordField apiSecretField;
  public Label credentialsStatus;
  public HBox createOrUpdateBox;
  public VBox customRoleSetupBox;
  public VBox existingRoleSelectorBox;
  public HBox apiCredentialsBox;
  public Button backToRoleSelectionButton;
  public TextField roleNameField;
  public TextField roleDescriptionField;
  public CheckBox teamAdminManageableCheckbox;
  public CheckBox jitAssignableCheckbox;
  public CheckBox jitAssignableDefaultCheckbox;
  public CheckBox isApiCheckbox;
  public CheckBox ignoreTeamRestrictionsCheckbox;
  public ListView<VeracodeRole> roleToEditListView;
  public TreeView<VeracodeRole> childRolesTreeView;
  public TreeView<Object> permissionsTreeView;
  public Button updateRolesButton;
  private ApiCredentials previousCredentials = null;
  private boolean isUpdating = false;

  private Set<VeracodeRole> availableRoles;
  private Set<VeracodePermission> availablePermissions;
  private boolean isUsingValidCredentials;

  private static boolean shouldDisablePermissionNode(VeracodePermission permission, boolean isApi) {
    return (!isApi && permission.apiOnly()) || (isApi && permission.uiOnly());
  }

  @FXML
  protected void fetchCredentialsFromProfile() {
    String profile = credentialsProfile.getText();
    ApiCredentials.fromProfile(profile)
        .ifPresent(apiCredentials -> {
          apiIdField.setText(apiCredentials.getApiId());
          apiSecretField.setText(apiCredentials.getApiKey());
        });
  }

  @FXML
  protected void initialize() {
    fetchCredentialsFromProfile();
    validateApiCredentials();
    roleToEditListView.setOnMouseClicked(this::handleListViewClick);
    isApiCheckbox.selectedProperty().addListener(this::recalculateAvailableRolesAndPermissionsIfNeeded);
    childRolesTreeView.setOnMouseClicked(this::handleChildRolesClick);
    permissionsTreeView.setOnMouseClicked(this::handlePermissionsClick);
    apiIdField.textProperty().addListener(this::invalidateCredentials);
    apiSecretField.textProperty().addListener(this::invalidateCredentials);
  }

  private void invalidateCredentials(Observable observable, String oldValue, String newValue) {
    if (!oldValue.equals(newValue)) {
      credentialsStatus.setText("Credentials not validated!");
      credentialsStatus.setTextFill(Color.RED);
    }
  }

  private void handleChildRolesClick(MouseEvent mouseClickEvent) {
    if (mouseClickEvent.getClickCount() == 2 && childRolesTreeView.getFocusModel().getFocusedItem() != null) {
      CheckBoxTreeItemExt<VeracodeRole> focusedItem = (CheckBoxTreeItemExt<VeracodeRole>) childRolesTreeView.getFocusModel().getFocusedItem();
      if (focusedItem.isEnabled()) {
        focusedItem.setSelected(true);
      }
    }
  }

  private void handlePermissionsClick(MouseEvent mouseClickEvent) {
    if (mouseClickEvent.getClickCount() == 2 && permissionsTreeView.getFocusModel().getFocusedItem() != null) {
      CheckBoxTreeItemExt<Object> focusedItem = (CheckBoxTreeItemExt<Object>) permissionsTreeView.getFocusModel().getFocusedItem();
      if (focusedItem.isEnabled()) {
        focusedItem.setSelected(true);
      }
    }
  }

  private void recalculateAvailableRolesAndPermissionsIfNeeded(Observable observable) {
    boolean isApi = isApiCheckbox.isSelected();
    permissionsTreeView.getRoot()
        .getChildren()
        .forEach(child -> {
          if (child instanceof CheckBoxTreeItemExt<?> && child.getValue() instanceof VeracodePermission) {
            ((CheckBoxTreeItemExt<Object>) child).setDisabled(
                shouldDisablePermissionNode((VeracodePermission) child.getValue(), isApi));
          }
        });
    childRolesTreeView.getRoot()
        .getChildren()
        .forEach(child -> {
          if (child instanceof CheckBoxTreeItemExt<?>) {
            ((CheckBoxTreeItemExt<VeracodeRole>) child).setDisabled(
                shouldDisableRoleNode(child.getValue(), isApi));
          }
        });
  }

  private boolean shouldDisableRoleNode(VeracodeRole role, boolean isApi) {
    return isApi ^ role.isApi();
  }

  @FXML
  protected void trySave() {
    VeracodeCustomRolesUIApplication.runWithWaitCursor(() -> {
      VeracodeRole currentRole = isUpdating ? roleToEditListView.getSelectionModel().getSelectedItem() : null;
      return VeracodeApi.trySaveCustomRole(previousCredentials, new VeracodeRole(
          currentRole == null ? null : currentRole.roleId(),
          currentRole == null ? -1 : currentRole.roleLegacyId(),
          roleNameField.getText(),
          roleDescriptionField.getText(),
          false,
          false,
          false,
          teamAdminManageableCheckbox.isSelected(),
          jitAssignableCheckbox.isSelected(),
          jitAssignableDefaultCheckbox.isSelected(),
          isApiCheckbox.isSelected(),
          false,
          ignoreTeamRestrictionsCheckbox.isSelected(),
          getAllSelectedPermissions(),
          getAllSelectedRoles()));
    }, (apiResults) -> {
      String httpAction = isUpdating ? "UPDATING" : "CREATING";
      if (apiResults.isEmpty()) {
        MessageHandler.showError("Unknown error when " + httpAction + " Custom Role");
      } else if (!apiResults.get().didSucceed()) {
        MessageHandler.showError("Error when " + httpAction + " Custom Role\n"
            + apiResults.get().getErrorMessage());
      } else {
        MessageHandler.showSuccess("Successfully " + (isUpdating ? "UPDATED" : "CREATED") + " role named '" + roleNameField.getText() + "'");
        reset();
      }
    });
  }

  private List<VeracodeRole> getAllSelectedRoles() {
    return childRolesTreeView.getRoot().getChildren()
        .stream()
        .filter(treeItem -> treeItem instanceof CheckBoxTreeItemExt<VeracodeRole>)
        .map(treeItem -> (CheckBoxTreeItemExt<VeracodeRole>) treeItem)
        .filter(CheckBoxTreeItem::isSelected)
        .map(TreeItem::getValue)
        .toList();
  }

  private List<VeracodePermission> getAllSelectedPermissions() {
    return permissionsTreeView.getRoot().getChildren()
        .stream()
        .filter(treeItem -> treeItem instanceof CheckBoxTreeItemExt<Object>)
        .map(treeItem -> (CheckBoxTreeItemExt<Object>) treeItem)
        .filter(CheckBoxTreeItem::isSelected)
        .filter(permissionItem -> permissionItem.getValue() instanceof VeracodePermission)
        .map(this::getSelectedPermissionFromTreeItem)
        .toList();
  }

  private VeracodePermission getSelectedPermissionFromTreeItem(CheckBoxTreeItemExt<Object> selectedPermission) {
    VeracodePermission oldPermission = (VeracodePermission) selectedPermission.getValue();
    return new VeracodePermission(
        oldPermission.permissionId(), oldPermission.permissionName(), oldPermission.permissionDescription(),
        oldPermission.customRoleEnabled(), oldPermission.apiOnly(), oldPermission.uiOnly(),
        getAllSelectedPermissionTypes(selectedPermission.getChildren()));
  }

  private List<String> getAllSelectedPermissionTypes(ObservableList<TreeItem<Object>> permissionTypes) {
    return permissionTypes.stream()
        .filter(treeItem -> treeItem instanceof CheckBoxTreeItemExt<Object>)
        .map(treeItem -> (CheckBoxTreeItemExt<Object>) treeItem)
        .filter(CheckBoxTreeItem::isSelected)
        .filter(treeItem -> treeItem.getValue() instanceof String)
        .map(treeItem -> (String) treeItem.getValue())
        .toList();
  }

  protected void handleListViewClick(MouseEvent mouseClickEvent) {
    if (mouseClickEvent.getClickCount() == 2) {
      selectExistingRole();
    }
  }

  private void loadAvailableRoles(List<VeracodeRole> selectedRoles) {
    boolean isApi = isApiCheckbox.isSelected();
    List<CheckBoxTreeItemExt<VeracodeRole>> checkboxItems =
        availableRoles.stream()
            .map(role -> {
              CheckBoxTreeItemExt<VeracodeRole> checkBoxTreeItem = new CheckBoxTreeItemExt<>(role);
              checkBoxTreeItem.setSelected(selectedRoles.contains(role));
              checkBoxTreeItem.setDisabled(shouldDisableRoleNode(role, isApi));
              return checkBoxTreeItem;
            })
            .toList();
    CheckBoxTreeItemExt<VeracodeRole> rootNode = new CheckBoxTreeItemExt<>(VeracodeRole.BASE_NODE);
    rootNode.getChildren().addAll(checkboxItems);
    rootNode.setExpanded(true);
    rootNode.setDisabled(true);

    childRolesTreeView.setRoot(rootNode);
    childRolesTreeView.setCellFactory(new CheckBoxTreeCellFactoryExt<>());
  }

  private void clearCustomRoleForm() {
    roleNameField.setText("");
    roleDescriptionField.setText("");
    teamAdminManageableCheckbox.setSelected(false);
    jitAssignableCheckbox.setSelected(true);
    jitAssignableDefaultCheckbox.setSelected(false);
    isApiCheckbox.setSelected(false);
    ignoreTeamRestrictionsCheckbox.setSelected(false);

    loadAvailableRoles(Collections.emptyList());
    loadAvailablePermissions(Collections.emptyList());
  }

  private void setVisibilities(boolean apiCredentialsVisibility, boolean roleSelectorVisibility,
                               boolean createOrUpdateVisibility, boolean customRoleSetupVisibility,
                               boolean backToSelectorButtonVisibility) {
    apiCredentialsBox.setVisible(apiCredentialsVisibility);
    existingRoleSelectorBox.setVisible(roleSelectorVisibility);
    createOrUpdateBox.setVisible(createOrUpdateVisibility);
    customRoleSetupBox.setVisible(customRoleSetupVisibility);
    backToRoleSelectionButton.setVisible(backToSelectorButtonVisibility);

    apiCredentialsBox.setManaged(apiCredentialsVisibility);
    existingRoleSelectorBox.setManaged(roleSelectorVisibility);
    createOrUpdateBox.setManaged(createOrUpdateVisibility);
    customRoleSetupBox.setManaged(customRoleSetupVisibility);
    backToRoleSelectionButton.setManaged(backToSelectorButtonVisibility);
  }

  @FXML
  protected void backToRoleSelection() {
    setVisibilities(false, true, false, false, false);
  }

  @FXML
  protected void startUpdateProcess() {
    roleToEditListView.setItems(getEditableRolesFromList(availableRoles));
    setVisibilities(false, true, false, false, true);
    isUpdating = true;
  }

  private ObservableList<VeracodeRole> getEditableRolesFromList(Set<VeracodeRole> availableRoles) {
    ObservableList<VeracodeRole> editableRoles = FXCollections.observableArrayList();
    editableRoles.addAll(
        availableRoles.stream()
            .filter(role -> role.roleName().startsWith(CUSTOM_ROLE_NAME_PREFIX))
            .toList());
    return editableRoles;
  }

  @FXML
  protected void selectExistingRole() {
    VeracodeRole selectedItem = roleToEditListView.getSelectionModel().getSelectedItem();
    if (selectedItem == null) {
      return;
    }
    isUpdating = true;

    roleNameField.setText(StringUtils.substringAfter(selectedItem.roleName(), CUSTOM_ROLE_NAME_PREFIX));
    roleDescriptionField.setText(selectedItem.roleDescription());
    teamAdminManageableCheckbox.setSelected(selectedItem.teamAdminManageable());
    jitAssignableCheckbox.setSelected(selectedItem.jitAssignable());
    jitAssignableDefaultCheckbox.setSelected(selectedItem.jitAssignableDefault());
    isApiCheckbox.setSelected(selectedItem.isApi());
    ignoreTeamRestrictionsCheckbox.setSelected(selectedItem.ignoreTeamRestrictions());

    loadAvailableRoles(selectedItem.childRoles());
    loadAvailablePermissions(selectedItem.permissions());

    setVisibilities(false, false, false, true, true);
  }

  private void loadAvailablePermissions(List<VeracodePermission> selectedPermissions) {
    boolean isApi = isApiCheckbox.isSelected();
    List<CheckBoxTreeItemExt<Object>> checkboxItems =
        availablePermissions.stream()
            .filter(VeracodePermission::customRoleEnabled)
            .map(permission -> {
              CheckBoxTreeItemExt<Object> checkBoxTreeItem = new CheckBoxTreeItemExt<>(permission);
              checkBoxTreeItem.setSelected(selectedPermissions.contains(permission));
              if (permission.permissionTypes() != null && !permission.permissionTypes().isEmpty()) {
                checkBoxTreeItem.getChildren().addAll(permission.permissionTypes().stream()
                    .filter(permissionType -> !permissionType.equals("admin"))
                    .map(permissionType -> new CheckBoxTreeItemExt<Object>(permissionType))
                    .toList());
              }
              checkBoxTreeItem.setDisabled(shouldDisablePermissionNode(permission, isApi));
              checkBoxTreeItem.setExpanded(true);
              return checkBoxTreeItem;
            })
            .toList();

    CheckBoxTreeItemExt<Object> rootNode = new CheckBoxTreeItemExt<>(VeracodePermission.BASE_NODE);
    rootNode.getChildren().addAll(checkboxItems);
    rootNode.setExpanded(true);
    rootNode.setDisabled(true);

    permissionsTreeView.setRoot(rootNode);
    permissionsTreeView.setCellFactory(new CheckBoxTreeCellFactoryExt<>());
  }

  @FXML
  protected void startCreationProcess() {
    isUpdating = false;
    clearCustomRoleForm();
    setVisibilities(false, false, false, true, false);
  }

  @FXML
  protected void reset() {
    setVisibilities(true, false, true, false, false);
  }

  @FXML
  protected void validateApiCredentials() {
    VeracodeCustomRolesUIApplication.runWithWaitCursor(() -> {
      Optional<ApiCredentials> newCredentials = ApiCredentials.fromIdAndKey(apiIdField.getText(), apiSecretField.getText());
      if (newCredentials.map(credentials -> credentials.equals(previousCredentials)).orElse(false)) {
        return isUsingValidCredentials;
      }
      previousCredentials = newCredentials.orElse(null);
      return newCredentials
          .map(VeracodeApi::validateCredentials)
          .orElse(false);
    }, (isValidCredentials) -> {
      isUsingValidCredentials = isValidCredentials;
      if (isValidCredentials) {
        credentialsStatus.setText("Valid Credentials");
        credentialsStatus.setTextFill(Color.GREEN);
        reset();
        updateRolesAndPermissions();
        updateRolesButton.setDisable(false);
      } else {
        credentialsStatus.setText("Invalid Credentials");
        credentialsStatus.setTextFill(Color.RED);
        setVisibilities(true, false, false, false, false);
        updateRolesButton.setDisable(true);
      }
    });
  }

  @FXML
  protected void updateRolesAndPermissions() {
    VeracodeCustomRolesUIApplication.runWithWaitCursor(() -> {
      availableRoles = VeracodeApi.getAllRoles(previousCredentials);
      availablePermissions = VeracodeApi.getAllPermissions(previousCredentials);
    });
  }
}