package com.cadonuno.veracodecustomrolesui.models;

import org.apache.sling.commons.json.JSONObject;

import java.util.Collections;
import java.util.List;

import static com.cadonuno.veracodecustomrolesui.api.VeracodeApi.*;

public record VeracodePermission(String permissionId, String permissionName, String permissionDescription,
                                 boolean customRoleEnabled, boolean apiOnly, boolean uiOnly,
                                 List<String> permissionTypes) implements Comparable<VeracodePermission> {

  public static final VeracodePermission BASE_NODE = new VeracodePermission(null, null, "[Select Permissions]",
      false, false, false, Collections.emptyList());

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
    return permissionDescription.compareTo(other.permissionDescription);
  }
}
