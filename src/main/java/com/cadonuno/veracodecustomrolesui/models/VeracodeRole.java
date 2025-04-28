package com.cadonuno.veracodecustomrolesui.models;

import com.cadonuno.veracodecustomrolesui.api.VeracodeApi;
import org.apache.sling.commons.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.cadonuno.veracodecustomrolesui.api.VeracodeApi.*;

public record VeracodeRole(String roleId, int roleLegacyId, String roleName, String roleDescription,
                           boolean isInternal, boolean requiresToken, boolean assignedToProxyUsers,
                           boolean teamAdminManageable, boolean jitAssignable,
                           boolean jitAssignableDefault, boolean isApi, boolean isScanType,
                           boolean ignoreTeamRestrictions, List<VeracodePermission> permissions,
                           List<VeracodeRole> childRoles) implements Comparable<VeracodeRole> {

  public static final VeracodeRole BASE_NODE = new VeracodeRole(null, -1, null, "[Select Child Roles]",
      false, false, false, false, false, false,
      false, false, false, Collections.emptyList(), Collections.emptyList());

  public static VeracodeRole fromJsonObject(JSONObject roleNode) {
    return new VeracodeRole(
        tryGetElementAsString(roleNode, "role_id").orElse(""),
        tryGetElementAsInteger(roleNode, "role_legacy_id").orElse(0),
        tryGetElementAsString(roleNode, "role_name").orElse(""),
        tryGetElementAsString(roleNode, "role_description").orElse(""),
        tryGetElementAsBoolean(roleNode, "is_internal").orElse(false),
        tryGetElementAsBoolean(roleNode, "requires_token").orElse(false),
        tryGetElementAsBoolean(roleNode, "assigned_to_proxy_users").orElse(false),
        tryGetElementAsBoolean(roleNode, "team_admin_manageable").orElse(false),
        tryGetElementAsBoolean(roleNode, "jit_assignable").orElse(false),
        tryGetElementAsBoolean(roleNode, "jit_assignable_default").orElse(false),
        tryGetElementAsBoolean(roleNode, "is_api").orElse(false),
        tryGetElementAsBoolean(roleNode, "is_scan_type").orElse(false),
        tryGetElementAsBoolean(roleNode, "ignore_team_restrictions").orElse(false),
        getArrayNode(roleNode, "permissions")
            .map(VeracodeApi::getPermissionsFromNode)
            .orElse(Collections.emptyList()),
        getArrayNode(roleNode, "child_roles")
            .map(VeracodeApi::getRolesFromNode)
            .orElse(Collections.emptyList()));
  }

  @Override
  public String toString() {
    return this.roleDescription;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VeracodeRole that = (VeracodeRole) o;
    return roleLegacyId == that.roleLegacyId && Objects.equals(roleId, that.roleId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roleId, roleLegacyId);
  }

  @Override
  public int compareTo(VeracodeRole other) {
    return roleDescription.compareTo(other.roleDescription);
  }

  public String toJsonParameter() {
    StringBuilder jsonBuilder = new StringBuilder("{\n");
    addParameter(jsonBuilder, "role_name", roleName, false);
    addParameter(jsonBuilder, "role_description", roleDescription, false);
    addParameter(jsonBuilder, "team_admin_manageable", teamAdminManageable, false);
    addParameter(jsonBuilder, "jit_assignable", jitAssignable, false);
    addParameter(jsonBuilder, "jit_assignable_default", jitAssignableDefault, false);
    addParameter(jsonBuilder, "is_api", isApi, false);
    addParameter(jsonBuilder, "ignore_team_restrictions", ignoreTeamRestrictions, false);
    addParameter(jsonBuilder, "permissions", permissions, false);
    addParameter(jsonBuilder, "child_roles", childRoles, true);
    jsonBuilder.append("}");
    return jsonBuilder.toString();
  }

  private void addParameter(StringBuilder jsonBuilder,
                            String parameterName,
                            Object toAdd,
                            boolean isLast) {
    jsonBuilder.append("\"").append(parameterName).append("\": ");
    if (toAdd instanceof String) {
      jsonBuilder.append("\"").append(toAdd).append("\"");
    } else if (toAdd instanceof Boolean) {
      jsonBuilder.append(toAdd);
    } else if (toAdd instanceof List<?>) {
      addListToParameter(jsonBuilder, (List<?>) toAdd);
    }
    if (!isLast) {
      jsonBuilder.append(",\n");
    }
  }

  private void addListToParameter(StringBuilder jsonBuilder, List<?> toAdd) {
    if (toAdd.isEmpty()) {
      jsonBuilder.append("[]");
    } else {
      jsonBuilder.append("[\n");
      Object firstElement = toAdd.get(0);
      int addedElements = 1;
      int totalElements = toAdd.size();
      if (firstElement instanceof VeracodePermission) {
        for (Object permissionObj : toAdd) {
          VeracodePermission permission = (VeracodePermission) permissionObj;
          jsonBuilder.append("{\n");
          addParameter(jsonBuilder, "permission_name", permission.permissionName(), permission.permissionTypes().isEmpty());
          if (!permission.permissionTypes().isEmpty()) {
            addParameter(jsonBuilder, "permission_types", permission.permissionTypes(), true);
          }
          jsonBuilder.append("}");
          if (addedElements < totalElements) {
            jsonBuilder.append(",\n");
          }
          addedElements++;
        }
      } else if (firstElement instanceof VeracodeRole) {
        for (Object roleObj : toAdd) {
          VeracodeRole role = (VeracodeRole) roleObj;
          jsonBuilder.append("{\n");
          addParameter(jsonBuilder, "role_name", role.roleName(), true);
          jsonBuilder.append("}");

          if (addedElements < totalElements) {
            jsonBuilder.append(",\n");
          }
          addedElements++;
        }
      } else if (firstElement instanceof String) {
        for (Object permissionTypeObj : toAdd) {
          String permissionType = (String) permissionTypeObj;
          jsonBuilder.append("\"").append(permissionType).append("\"");
          if (addedElements < totalElements) {
            jsonBuilder.append(",\n");
          }
          addedElements++;
        }
      }
      jsonBuilder.append("]");
    }
  }
}
