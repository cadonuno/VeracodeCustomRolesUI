package com.cadonuno.veracodecustomrolesui.ui;

import com.cadonuno.veracodecustomrolesui.MessageHandler;
import com.cadonuno.veracodecustomrolesui.VeracodeCustomRolesUIApplication;
import com.cadonuno.veracodecustomrolesui.api.ApiCredentials;
import com.cadonuno.veracodecustomrolesui.api.ApiResults;
import com.cadonuno.veracodecustomrolesui.api.VeracodeApi;
import com.cadonuno.veracodecustomrolesui.models.VeracodePermission;
import com.cadonuno.veracodecustomrolesui.models.VeracodeRole;
import com.cadonuno.veracodecustomrolesui.services.RoleExportImportService;
import com.cadonuno.veracodecustomrolesui.ui.components.CheckBoxTreeCellFactoryExt;
import com.cadonuno.veracodecustomrolesui.ui.components.CheckBoxTreeItemExt;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.commons.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class VeracodeCustomRolesUIController {
  private static final int TEXT_FORMAT_LENGTH = 256;
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

  private TextFormatter<?> getTextFormatter() {
    UnaryOperator<TextFormatter.Change> unaryOperator = (TextFormatter.Change change) -> {
      if (change.isContentChange()) {
        int newLength = change.getControlNewText().length();
        if (newLength > TEXT_FORMAT_LENGTH) {
          String newChangeText = change.getControlNewText().substring(0, TEXT_FORMAT_LENGTH);
          change.setText(newChangeText);
          change.setRange(0, TEXT_FORMAT_LENGTH);
        }
      }
      return change;
    };
    return new TextFormatter<>(unaryOperator);
  }

  @SuppressWarnings("unused")
  @FXML
  protected void initialize() {
    fetchCredentialsFromProfile();
    validateApiCredentials();
    roleToEditListView.setOnMouseClicked(this::handleListViewClick);
    roleToEditListView.setOnKeyReleased(this::handleRoleListPress);
    isApiCheckbox.selectedProperty().addListener(this::recalculateAvailableRolesAndPermissionsIfNeeded);
    childRolesTreeView.setOnMouseClicked(this::handleChildRolesClick);
    permissionsTreeView.setOnMouseClicked(this::handlePermissionsClick);
    apiIdField.textProperty().addListener(this::invalidateCredentials);
    apiSecretField.textProperty().addListener(this::invalidateCredentials);
    roleNameField.setTextFormatter(getTextFormatter());
    roleDescriptionField.setTextFormatter(getTextFormatter());
  }

  private void handleRoleListPress(KeyEvent keyEvent) {
    if (keyEvent.getCode() == KeyCode.DELETE) {
      tryDeleteRole();
    }
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

  @SuppressWarnings("unused")
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
        teamAdminManageableCheckbox.isSelected(),
        jitAssignableCheckbox.isSelected(),
        jitAssignableDefaultCheckbox.isSelected(),
        isApiCheckbox.isSelected(),
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
        .filter(treeItem -> treeItem.isSelected() || !(getAllSelectedPermissionTypes(treeItem.getChildren()).isEmpty()))
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

  @SuppressWarnings("unused")
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

  @SuppressWarnings("SuspiciousMethodCalls")
  private void loadAvailablePermissions(List<VeracodePermission> selectedPermissions) {
    boolean isApi = isApiCheckbox.isSelected();
    List<CheckBoxTreeItemExt<Object>> checkboxItems =
        availablePermissions.stream()
            //.filter(VeracodePermission::isCustomRoleEnabled)
            .map(permission -> {
              CheckBoxTreeItemExt<Object> checkBoxTreeItem = new CheckBoxTreeItemExt<>(permission);
              Optional<VeracodePermission> selectedPermission = selectedPermissions
                      .stream()
                      .filter(p -> p.equals(permission))
                      .findFirst();

              checkBoxTreeItem.setSelected(selectedPermission.isPresent());
              if (permission.getPermissionTypes() != null && !permission.getPermissionTypes().isEmpty()) {
                checkBoxTreeItem.getChildren().addAll(permission.getPermissionTypes().stream()
                    .filter(permissionType -> !permissionType.equals("admin"))
                    .map(permissionType -> new CheckBoxTreeItemExt<Object>(permissionType))
                    .collect(Collectors.toList()));
                selectedPermission
                        .ifPresent(permissionInfo -> {
                          for (TreeItem<Object> permissionType : checkBoxTreeItem.getChildren()) {
                            ((CheckBoxTreeItemExt<Object>)permissionType).setSelected(permissionInfo.getPermissionTypes().contains(permissionType.getValue()));
                          }
                        });
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

  @SuppressWarnings("unused")
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
        try {
            Optional<ApiCredentials> newCredentials = ApiCredentials.fromIdAndKey(apiIdField.getText(), apiSecretField.getText());
            if (newCredentials.map(credentials -> credentials.equals(previousCredentials)).orElse(false)) {
                return isUsingValidCredentials;
            }
            previousCredentials = newCredentials.orElse(null);
            return newCredentials
                    .map(VeracodeApi::validateCredentials)
                    .orElse(false);
        } catch (Exception e) {
            MessageHandler.showError(e);
            return false;
        }
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
    updateRoles(true);
  }

  private void updateRoles(boolean includePermissions) {
    VeracodeCustomRolesUIApplication.runWithWaitCursor(() -> {
      availableRoles = VeracodeApi.getAllRoles(previousCredentials);
      if (includePermissions) {
        availablePermissions = VeracodeApi.getAllPermissions(previousCredentials);
      }
      return "";
    }, (callback) -> roleToEditListView.setItems(getEditableRolesFromList(availableRoles)));
  }

  @SuppressWarnings("unused")
  @FXML
  protected void exportSelectedRoles() {
    ObservableList<VeracodeRole> editableRoles = getEditableRolesFromList(availableRoles);
    if (editableRoles.isEmpty()) {
      MessageHandler.showError("No roles available to export");
      return;
    }

    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.setTitle("Export Roles");
    dialog.setHeaderText("Select roles to export (Ctrl+Click or Shift+Click for multiple)");

    ListView<VeracodeRole> listView = new ListView<>(editableRoles);
    listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    listView.setPrefHeight(300);

    dialog.getDialogPane().setContent(listView);
    ButtonType exportButtonType = new ButtonType("Export", ButtonBar.ButtonData.OK_DONE);
    ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
    dialog.getDialogPane().getButtonTypes().addAll(exportButtonType, cancelButtonType);

    Optional<ButtonType> result = dialog.showAndWait();
    if (result.isPresent() && result.get() == exportButtonType) {
      List<VeracodeRole> selectedRoles = new ArrayList<>(listView.getSelectionModel().getSelectedItems());

      if (selectedRoles.isEmpty()) {
        MessageHandler.showError("Please select at least one role to export");
        return;
      }

      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Save Exported Roles");
      fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
      fileChooser.setInitialFileName("exported_roles.json");

      Stage stage = (Stage) roleToEditListView.getScene().getWindow();
      File selectedFile = fileChooser.showSaveDialog(stage);

      if (selectedFile != null) {
        VeracodeCustomRolesUIApplication.runWithWaitCursor(
            () -> {
              try {
                RoleExportImportService.exportRolesToJson(selectedRoles, selectedFile.toPath());
                return true;
              } catch (Exception e) {
                MessageHandler.showError("Error exporting roles: " + e.getMessage());
                return false;
              }
            },
            (success) -> {
              if (success) {
                MessageHandler.showSuccess("Successfully exported " + selectedRoles.size() + " role(s) to " + selectedFile.getName());
              }
            }
        );
      }
    }
  }

  @SuppressWarnings("unused")
  @FXML
  protected void importRoles() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Select Roles File to Import");
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));

    Stage stage = (Stage) roleToEditListView.getScene().getWindow();
    File selectedFile = fileChooser.showOpenDialog(stage);

    if (selectedFile != null) {
      VeracodeCustomRolesUIApplication.runWithWaitCursor(
          () -> {
            try {
              List<RoleExportImportService.RoleImportData> importedRoles =
                  RoleExportImportService.importRolesFromJson(selectedFile.toPath());

              // Validate child_roles references
              List<String> missingChildRoles =
                  RoleExportImportService.validateChildRoles(importedRoles, availableRoles);
              if (!missingChildRoles.isEmpty()) {
                String errorMessage = "Invalid Child Roles\n\nThe following child roles referenced in the import file do not exist:\n\n" +
                    String.join("\n", missingChildRoles) +
                    "\n\nPlease ensure all referenced roles exist on the platform before importing.";
                MessageHandler.showError(errorMessage);
                return null;
              }

              return importedRoles;
            } catch (JSONException | IOException e) {
              MessageHandler.showError("Error importing roles: " + e.getMessage());
              return null;
            }
          },
          (importedRoles) -> {
            if (importedRoles != null) {
              handleImportConflicts(importedRoles);
            }
          }
      );
    }
  }

  private void handleImportConflicts(List<RoleExportImportService.RoleImportData> importedRoles) {
    List<RoleExportImportService.RoleImportData> sortedRoles = topologicalSort(importedRoles);
    List<RoleExportImportService.RoleImportData> conflicts = new ArrayList<>();
    List<RoleExportImportService.RoleImportData> noConflicts = new ArrayList<>();

    for (RoleExportImportService.RoleImportData roleData : sortedRoles) {
      if (findRoleByName(roleData.roleName).isPresent()) {
        conflicts.add(roleData);
      } else {
        noConflicts.add(roleData);
      }
    }

    if (!conflicts.isEmpty()) {
      Platform.runLater(() -> showConflictDialog(conflicts, noConflicts));
    } else {
      VeracodeCustomRolesUIApplication.runWithWaitCursor(
          () -> {
            noConflicts.forEach(roleData -> createImportedRole(roleData, roleData.roleName, roleData.roleDescription));
            availableRoles = VeracodeApi.getAllRoles(previousCredentials);
            return true;
          },
          (success) -> roleToEditListView.setItems(getEditableRolesFromList(availableRoles))
      );
    }
  }

  private List<RoleExportImportService.RoleImportData> topologicalSort(List<RoleExportImportService.RoleImportData> roles) {
    Map<String, RoleExportImportService.RoleImportData> rolesByName = new HashMap<>();
    List<RoleExportImportService.RoleImportData> sorted = new ArrayList<>();
    Set<String> visited = new HashSet<>();
    Set<String> visiting = new HashSet<>();

    for (RoleExportImportService.RoleImportData role : roles) {
      rolesByName.putIfAbsent(RoleExportImportService.toLookupKey(role.roleName), role);
    }

    for (RoleExportImportService.RoleImportData role : roles) {
      visit(role, rolesByName, sorted, visited, visiting);
    }

    return sorted;
  }

  private Optional<VeracodeRole> findRoleByName(String name) {
    return availableRoles.stream()
        .filter(r -> r.getRoleName() != null && r.getRoleName().equalsIgnoreCase(name))
        .findFirst();
  }

  private void visit(RoleExportImportService.RoleImportData role,
                     Map<String, RoleExportImportService.RoleImportData> rolesByName,
                     List<RoleExportImportService.RoleImportData> sorted,
                     Set<String> visited, Set<String> visiting) {
    if (role == null || role.roleName == null || role.roleName.isEmpty()) {
      return;
    }
    if (visited.contains(role.roleName)) {
      return;
    }
    if (visiting.contains(role.roleName)) {
      throw new IllegalArgumentException("Circular dependency detected: role '" + role.roleName + "' references itself");
    }

    visiting.add(role.roleName);

    for (VeracodeRole childRole : role.getChildRoles()) {
      RoleExportImportService.RoleImportData dependency = rolesByName.get(RoleExportImportService.toLookupKey(childRole.getRoleName()));
      if (dependency != null) {
        visit(dependency, rolesByName, sorted, visited, visiting);
      }
    }

    visiting.remove(role.roleName);
    visited.add(role.roleName);
    sorted.add(role);
  }

  private void showConflictDialog(List<RoleExportImportService.RoleImportData> conflicts,
                                   List<RoleExportImportService.RoleImportData> noConflicts) {
    Dialog<List<ConflictResolution>> dialog = new Dialog<>();
    dialog.setTitle("Import Conflicts");
    dialog.setHeaderText("The following roles already exist. Choose an action for each:");

    VBox content = new VBox(10);
    content.setPadding(new Insets(15));
    List<ConflictRow> rows = new ArrayList<>();

    for (RoleExportImportService.RoleImportData roleData : conflicts) {
      HBox row = new HBox(10);
      row.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0; -fx-padding: 10;");

      Label roleLabel = new Label(roleData.roleName);
      roleLabel.setMinWidth(150);

      ComboBox<String> actionBox = new ComboBox<>();
      actionBox.getItems().addAll("Overwrite", "Skip", "Rename");
      actionBox.setValue("Skip");

      TextField renameField = new TextField();
      renameField.setPromptText("New name");
      renameField.setMinWidth(150);
      renameField.setVisible(false);
      renameField.setManaged(false);
      renameField.setTextFormatter(getTextFormatter());

      actionBox.valueProperty().addListener((obs, oldVal, newVal) -> {
        boolean isRename = "Rename".equals(newVal);
        renameField.setVisible(isRename);
        renameField.setManaged(isRename);
      });

      row.getChildren().addAll(roleLabel, actionBox, renameField);
      content.getChildren().add(row);

      rows.add(new ConflictRow(roleData, actionBox, renameField));
    }

    ScrollPane scrollPane = new ScrollPane(content);
    scrollPane.setFitToWidth(true);
    scrollPane.setPrefHeight(300);

    dialog.getDialogPane().setContent(scrollPane);
    ButtonType applyButton = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
    ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
    dialog.getDialogPane().getButtonTypes().addAll(applyButton, cancelButton);

    dialog.setResultConverter(buttonType -> buttonType == applyButton ? snapshotResolutions(rows) : null);

    Optional<List<ConflictResolution>> result = dialog.showAndWait();
    if (!result.isPresent()) {
      return;
    }
    List<ConflictResolution> resolutions = result.get();

    VeracodeCustomRolesUIApplication.runWithWaitCursor(
        () -> {
          for (ConflictResolution resolution : resolutions) {
            if ("Overwrite".equals(resolution.action)) {
              deleteExistingRoleAndCreate(resolution.roleData);
            } else if ("Rename".equals(resolution.action) && !resolution.newName.isEmpty()) {
              String descWithoutPrefix = resolution.newName.startsWith(CUSTOM_ROLE_NAME_PREFIX) ?
                  resolution.newName.substring(CUSTOM_ROLE_NAME_PREFIX.length()) : resolution.newName;
              String nameWithPrefix = RoleExportImportService.ensureCustomPrefix(resolution.newName, CUSTOM_ROLE_NAME_PREFIX);
              createImportedRole(resolution.roleData, nameWithPrefix, descWithoutPrefix);
            }
          }

          for (RoleExportImportService.RoleImportData roleData : noConflicts) {
            createImportedRole(roleData, roleData.roleName, roleData.roleDescription);
          }

          availableRoles = VeracodeApi.getAllRoles(previousCredentials);
          return true;
        },
        (success) -> roleToEditListView.setItems(getEditableRolesFromList(availableRoles))
    );
  }

  /**
   * Reads the conflict dialog's controls on the JavaFX thread so the background import task only
   * sees plain data and does not keep the closed dialog's scene graph alive.
   */
  private static List<ConflictResolution> snapshotResolutions(List<ConflictRow> rows) {
    List<ConflictResolution> resolutions = new ArrayList<>(rows.size());
    for (ConflictRow row : rows) {
      resolutions.add(new ConflictResolution(row.roleData, row.actionBox.getValue(),
          row.renameField.getText().trim()));
    }
    return resolutions;
  }

  private static class ConflictRow {
    final RoleExportImportService.RoleImportData roleData;
    final ComboBox<String> actionBox;
    final TextField renameField;

    ConflictRow(RoleExportImportService.RoleImportData roleData, ComboBox<String> actionBox, TextField renameField) {
      this.roleData = roleData;
      this.actionBox = actionBox;
      this.renameField = renameField;
    }
  }

  private static class ConflictResolution {
    final RoleExportImportService.RoleImportData roleData;
    final String action;
    final String newName;

    ConflictResolution(RoleExportImportService.RoleImportData roleData, String action, String newName) {
      this.roleData = roleData;
      this.action = action;
      this.newName = newName;
    }
  }

  private void deleteExistingRoleAndCreate(RoleExportImportService.RoleImportData roleData) {
    Optional<VeracodeRole> existingRole = findRoleByName(roleData.roleName);

    if (existingRole.isPresent()) {
      try {
        Optional<ApiResults> deleteResult = VeracodeApi.tryDeleteRole(previousCredentials, existingRole.get().getRoleId());
        if (deleteResult.isPresent() && deleteResult.get().didFail()) {
          MessageHandler.showError("Error deleting existing role: " + deleteResult.get().getErrorMessage());
          return;
        }
        availableRoles.remove(existingRole.get());
        createImportedRole(roleData, roleData.roleName, roleData.roleDescription);
      } catch (Exception e) {
        MessageHandler.showError("Error deleting existing role: " + e.getMessage());
      }
    }
  }

  private void createImportedRole(RoleExportImportService.RoleImportData roleData, String roleName, String roleDescription) {
    try {
      if (roleName == null || roleName.trim().isEmpty()) {
        MessageHandler.showError("Cannot import role: name is empty");
        return;
      }
      String roleNameWithPrefix = RoleExportImportService.ensureCustomPrefix(roleName, CUSTOM_ROLE_NAME_PREFIX);
      List<VeracodePermission> permissions = roleData.getPermissions();
      List<VeracodeRole> childRoles = roleData.getChildRoles();
      Optional<ApiResults> result = VeracodeApi.trySaveCustomRole(previousCredentials,
          new VeracodeRole(null, -1, roleNameWithPrefix, roleDescription,
              roleData.teamAdminManageable, roleData.jitAssignable, roleData.jitAssignableDefault,
              roleData.isApi, roleData.ignoreTeamRestrictions,
              permissions, childRoles));
      if (result.isPresent() && result.get().didFail()) {
        MessageHandler.showError("Error creating role '" + roleName + "': " + result.get().getErrorMessage());
      }
    } catch (Exception e) {
      MessageHandler.showError("Error creating role '" + roleName + "': " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
    }
  }
}
