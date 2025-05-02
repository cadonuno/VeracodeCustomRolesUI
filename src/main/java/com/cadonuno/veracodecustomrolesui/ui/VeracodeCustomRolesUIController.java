package com.cadonuno.veracodecustomrolesui.ui;

import com.cadonuno.veracodecustomrolesui.MessageHandler;
import com.cadonuno.veracodecustomrolesui.VeracodeCustomRolesUIApplication;
import com.cadonuno.veracodecustomrolesui.api.ApiCredentials;
import com.cadonuno.veracodecustomrolesui.api.ApiResults;
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
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

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
    return (!isApi && permission.isApiOnly()) || (isApi && permission.isUiOnly());
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

  private TextFormatter<?> getTextFormatter(int length) {
    UnaryOperator<TextFormatter.Change> unaryOperator = (TextFormatter.Change change) -> {
      if (change.isContentChange()) {
        int newLength = change.getControlNewText().length();
        if (newLength > length) {
          String newChangeText = change.getControlNewText().substring(0, length);
          change.setText(newChangeText);
          change.setRange(0, length);
        }
      }
      return change;
    };
    return new TextFormatter<>(unaryOperator);
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
    roleNameField.setTextFormatter(getTextFormatter(256));
    roleDescriptionField.setTextFormatter(getTextFormatter(256));
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
    Optional.ofNullable(permissionsTreeView.getRoot())
        .map(TreeItem::getChildren)
        .ifPresent(root -> root.forEach(child -> {
          if (child instanceof CheckBoxTreeItemExt<?> && child.getValue() instanceof VeracodePermission) {
            ((CheckBoxTreeItemExt<Object>) child).setDisabled(
                shouldDisablePermissionNode((VeracodePermission) child.getValue(), isApi));
          }
        }));
    Optional.ofNullable(childRolesTreeView.getRoot())
        .map(TreeItem::getChildren)
        .ifPresent(root -> root.forEach(child -> {
          if (child instanceof CheckBoxTreeItemExt<?>) {
            ((CheckBoxTreeItemExt<VeracodeRole>) child).setDisabled(
                shouldDisableRoleNode(child.getValue(), isApi));
          }
        }));
  }

  private boolean shouldDisableRoleNode(VeracodeRole role, boolean isApi) {
    return isApi ^ role.isApi();
  }

  @FXML
  protected void trySave() {
    VeracodeRole currentRole = isUpdating ? roleToEditListView.getSelectionModel().getSelectedItem() : null;
    VeracodeRole veracodeRoleToSave = getVeracodeRoleToSave(currentRole);
    VeracodeCustomRolesUIApplication.runWithWaitCursor(
        () -> VeracodeApi.trySaveCustomRole(previousCredentials, veracodeRoleToSave), (apiResults) -> {
          String httpAction = isUpdating ? "UPDATING" : "CREATING";
          if (!apiResults.isPresent()) {
            MessageHandler.showError("Unknown error when " + httpAction + " Custom Role");
          } else if (apiResults.get().didFail()) {
            MessageHandler.showError("Error when " + httpAction + " Custom Role\n"
                + apiResults.get().getErrorMessage());
          } else {
            updateRoleInAvailableRolesList(apiResults.get(), veracodeRoleToSave, currentRole);
          }
        });
  }

  private void updateRoleInAvailableRolesList(ApiResults apiResults, VeracodeRole veracodeRoleToSave, VeracodeRole currentRole) {
    MessageHandler.showSuccess("Successfully " + (isUpdating ? "UPDATED" : "CREATED") + " role '" + roleNameField.getText() + "'/'" + roleDescriptionField.getText() + "'");

    if (isUpdating) {
      if (!veracodeRoleToSave.getRoleName().startsWith(CUSTOM_ROLE_NAME_PREFIX)) {
        veracodeRoleToSave.setRoleName(CUSTOM_ROLE_NAME_PREFIX + veracodeRoleToSave.getRoleName());
      }
      veracodeRoleToSave.setRoleName(veracodeRoleToSave.getRoleName().replace(" ", ""));
      availableRoles.remove(currentRole);
      availableRoles.add(veracodeRoleToSave);
    } else {
      availableRoles.add(VeracodeRole.fromJsonObject(apiResults.getApiResponse()));
    }
    loadCustomRoleList();
    reset();
  }

  private VeracodeRole getVeracodeRoleToSave(VeracodeRole currentRole) {
    return new VeracodeRole(
        currentRole == null ? null : currentRole.getRoleId(),
        currentRole == null ? -1 : currentRole.getRoleLegacyId(),
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
        getAllSelectedRoles());
  }

  private List<VeracodeRole> getAllSelectedRoles() {
    return childRolesTreeView.getRoot().getChildren()
        .stream()
        .filter(treeItem -> treeItem instanceof CheckBoxTreeItemExt<?>)
        .map(treeItem -> (CheckBoxTreeItemExt<VeracodeRole>) treeItem)
        .filter(CheckBoxTreeItem::isSelected)
        .map(TreeItem::getValue)
        .collect(Collectors.toList());
  }

  private List<VeracodePermission> getAllSelectedPermissions() {
    return permissionsTreeView.getRoot().getChildren()
        .stream()
        .filter(treeItem -> treeItem instanceof CheckBoxTreeItemExt<?>)
        .map(treeItem -> (CheckBoxTreeItemExt<Object>) treeItem)
        .filter(CheckBoxTreeItem::isSelected)
        .filter(permissionItem -> permissionItem.getValue() instanceof VeracodePermission)
        .map(this::getSelectedPermissionFromTreeItem)
        .collect(Collectors.toList());
  }

  private VeracodePermission getSelectedPermissionFromTreeItem(CheckBoxTreeItemExt<Object> selectedPermission) {
    VeracodePermission oldPermission = (VeracodePermission) selectedPermission.getValue();
    return new VeracodePermission(
        oldPermission.getPermissionId(), oldPermission.getPermissionName(), oldPermission.getPermissionDescription(),
        oldPermission.isCustomRoleEnabled(), oldPermission.isApiOnly(), oldPermission.isUiOnly(),
        getAllSelectedPermissionTypes(selectedPermission.getChildren()));
  }

  private List<String> getAllSelectedPermissionTypes(ObservableList<TreeItem<Object>> permissionTypes) {
    return permissionTypes.stream()
        .filter(treeItem -> treeItem instanceof CheckBoxTreeItemExt<?>)
        .map(treeItem -> (CheckBoxTreeItemExt<Object>) treeItem)
        .filter(CheckBoxTreeItem::isSelected)
        .filter(treeItem -> treeItem.getValue() instanceof String)
        .map(treeItem -> (String) treeItem.getValue())
        .collect(Collectors.toList());
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
            .collect(Collectors.toList());
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
    setVisibilities(true, true, true, false, true);
  }

  @FXML
  protected void loadCustomRoleList() {
    roleToEditListView.setItems(getEditableRolesFromList(availableRoles));
    setVisibilities(true, true, true, false, true);
    isUpdating = true;
  }

  private ObservableList<VeracodeRole> getEditableRolesFromList(Set<VeracodeRole> availableRoles) {
    ObservableList<VeracodeRole> editableRoles = FXCollections.observableArrayList();
    editableRoles.addAll(
        availableRoles.stream()
            .filter(role -> role.getRoleName().startsWith(CUSTOM_ROLE_NAME_PREFIX))
            .collect(Collectors.toList()));
    return editableRoles;
  }

  @FXML
  protected void selectExistingRole() {
    VeracodeRole selectedItem = roleToEditListView.getSelectionModel().getSelectedItem();
    if (selectedItem == null) {
      return;
    }
    isUpdating = true;

    roleNameField.setText(StringUtils.substringAfter(selectedItem.getRoleName(), CUSTOM_ROLE_NAME_PREFIX));
    roleDescriptionField.setText(selectedItem.getRoleDescription());
    teamAdminManageableCheckbox.setSelected(selectedItem.isTeamAdminManageable());
    jitAssignableCheckbox.setSelected(selectedItem.isJitAssignable());
    jitAssignableDefaultCheckbox.setSelected(selectedItem.isJitAssignableDefault());
    isApiCheckbox.setSelected(selectedItem.isApi());
    ignoreTeamRestrictionsCheckbox.setSelected(selectedItem.isIgnoreTeamRestrictions());

    loadAvailableRoles(selectedItem.getChildRoles());
    loadAvailablePermissions(selectedItem.getPermissions());

    setVisibilities(false, false, false, true, true);
  }

  @FXML
  protected void tryDeleteRole() {
    VeracodeRole selectedItem = roleToEditListView.getSelectionModel().getSelectedItem();
    if (selectedItem == null) {
      return;
    }
    if (MessageHandler.showConfirmation("Are you sure you want to delete role '" + selectedItem.getRoleDescription() + "'?")) {
      VeracodeCustomRolesUIApplication
          .runWithWaitCursor(() -> VeracodeApi.tryDeleteRole(previousCredentials, selectedItem.getRoleId()), (apiResults) -> {
            if (!apiResults.isPresent()) {
              MessageHandler.showError("Unknown error when deleting Custom Role");
            } else if (apiResults.get().didFail()) {
              MessageHandler.showError("Error when deleting Custom Role\n"
                  + apiResults.get().getErrorMessage());
            } else {
              MessageHandler.showSuccess("Successfully deleted role named '" + selectedItem.getRoleDescription() + "'");
              availableRoles.remove(selectedItem);
              loadCustomRoleList();
            }
          });
    }
  }

  private void loadAvailablePermissions(List<VeracodePermission> selectedPermissions) {
    boolean isApi = isApiCheckbox.isSelected();
    List<CheckBoxTreeItemExt<Object>> checkboxItems =
        availablePermissions.stream()
            .filter(VeracodePermission::isCustomRoleEnabled)
            .map(permission -> {
              CheckBoxTreeItemExt<Object> checkBoxTreeItem = new CheckBoxTreeItemExt<>(permission);
              checkBoxTreeItem.setSelected(selectedPermissions.contains(permission));
              if (permission.getPermissionTypes() != null && !permission.getPermissionTypes().isEmpty()) {
                checkBoxTreeItem.getChildren().addAll(permission.getPermissionTypes().stream()
                    .filter(permissionType -> !permissionType.equals("admin"))
                    .map(permissionType -> new CheckBoxTreeItemExt<Object>(permissionType))
                    .collect(Collectors.toList()));
              }
              checkBoxTreeItem.setDisabled(shouldDisablePermissionNode(permission, isApi));
              checkBoxTreeItem.setExpanded(true);
              return checkBoxTreeItem;
            })
            .collect(Collectors.toList());

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
    setVisibilities(true, true, true, false, false);
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
      return "";
    }, (callback) -> roleToEditListView.setItems(getEditableRolesFromList(availableRoles)));
  }
}