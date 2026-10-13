package com.cadonuno.veracodecustomrolesui.models;

import org.apache.sling.commons.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.cadonuno.veracodecustomrolesui.api.VeracodeApi.*;

public class VeracodePermission implements Comparable<VeracodePermission> {
  private final String permissionId;
  private final String permissionName;
  private final String permissionDescription;
  private final boolean customRoleEnabled;
  private final boolean apiOnly;
  private final boolean uiOnly;
  private final List<String> permissionTypes;

  public static final VeracodePermission BASE_NODE = new VeracodePermission(null, null, "[Select Permissions]",
      false, false, false, Collections.emptyList());

  public VeracodePermission(String permissionId, String permissionName, String permissionDescription,
                            boolean customRoleEnabled, boolean apiOnly, boolean uiOnly, List<String> permissionTypes) {
    this.permissionId = permissionId;
    this.permissionName = permissionName;
    this.permissionDescription = permissionDescription;
    this.customRoleEnabled = customRoleEnabled;
    this.apiOnly = apiOnly;
    this.uiOnly = uiOnly;
    this.permissionTypes = permissionTypes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VeracodePermission that = (VeracodePermission) o;
    return Objects.equals(permissionName, that.permissionName) && Objects.equals(permissionId, that.permissionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(permissionName, permissionId);
  }


  @SuppressWarnings("deprecation")
  public static VeracodePermission fromJsonObject(JSONObject permissionNode) {
    return new VeracodePermission(
        tryGetElementAsString(permissionNode, "permission_id").orElse(""),
        tryGetElementAsString(permissionNode, "permission_name").orElse(""),
        tryGetElementAsString(permissionNode, "permission_description").orElse(""),
        tryGetElementAsBoolean(permissionNode, "custom_role_enabled").orElse(false),
        tryGetElementAsBoolean(permissionNode, "api_only").orElse(false),
        tryGetElementAsBoolean(permissionNode, "ui_only").orElse(false),
        tryGetElementAsStringList(permissionNode, "permission_types").orElse(Collections.emptyList()));
  }

  @Override
  public String toString() {
    return permissionDescription;
  }


  @Override
  public int compareTo(VeracodePermission other) {
    return permissionDescription.toLowerCase().compareTo(other.permissionDescription.toLowerCase());
  }

  public String getPermissionId() {
    return permissionId;
  }

  public String getPermissionName() {
    return permissionName;
  }

  public String getPermissionDescription() {
    return permissionDescription;
  }

  public boolean isCustomRoleEnabled() {
    return customRoleEnabled;
  }

  public boolean isApiOnly() {
    return apiOnly;
  }

  public boolean isUiOnly() {
    return uiOnly;
  }

  public List<String> getPermissionTypes() {
    return new ArrayList<>(permissionTypes);
  }
}
