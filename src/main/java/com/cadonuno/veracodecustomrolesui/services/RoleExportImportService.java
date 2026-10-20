package com.cadonuno.veracodecustomrolesui.services;

import com.cadonuno.veracodecustomrolesui.models.VeracodePermission;
import com.cadonuno.veracodecustomrolesui.models.VeracodeRole;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class RoleExportImportService {
  private static final long MAX_IMPORT_FILE_BYTES = 10 * 1024 * 1024;

  public static void exportRolesToJson(List<VeracodeRole> roles, Path filePath) throws IOException {
    JSONArray rolesArray = new JSONArray();
    for (VeracodeRole role : roles) {
      rolesArray.put(roleToJsonObject(role));
    }

    try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
      writer.write(rolesArray.toString(2));
    }
  }

  public static List<RoleImportData> importRolesFromJson(Path filePath) throws IOException, JSONException {
    if (Files.size(filePath) > MAX_IMPORT_FILE_BYTES) {
      throw new IOException("Role import file exceeds 10MB limit");
    }
    JSONArray rolesArray = new JSONArray(new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8));

    List<RoleImportData> importedRoles = new ArrayList<>();
    for (int i = 0; i < rolesArray.length(); i++) {
      JSONObject roleObj = rolesArray.getJSONObject(i);
      importedRoles.add(new RoleImportData(roleObj));
    }
    return importedRoles;
  }

  public static List<String> validateChildRoles(
      List<RoleImportData> rolesFromFile,
      Set<VeracodeRole> allPlatformRoles) {

    Set<String> validRoleNames = new HashSet<>();
    for (RoleImportData roleData : rolesFromFile) {
      validRoleNames.add(toLookupKey(roleData.roleName));
    }
    for (VeracodeRole platformRole : allPlatformRoles) {
      validRoleNames.add(toLookupKey(platformRole.getRoleName()));
    }

    Set<String> missingRoles = new TreeSet<>();
    for (RoleImportData roleData : rolesFromFile) {
      for (VeracodeRole childRole : roleData.getChildRoles()) {
        String childRoleName = childRole.getRoleName();
        if (!validRoleNames.contains(toLookupKey(childRoleName))) {
          missingRoles.add(childRoleName);
        }
      }
    }

    return new ArrayList<>(missingRoles);
  }

  public static String toLookupKey(String roleName) {
    return Objects.requireNonNullElse(roleName, "").toLowerCase(Locale.ROOT);
  }

  public static String ensureCustomPrefix(String roleName, String prefix) {
    return roleName != null && roleName.startsWith(prefix) ? roleName : prefix + roleName;
  }

  private static JSONObject roleToJsonObject(VeracodeRole role) throws JSONException {
    JSONObject obj = new JSONObject();
    obj.put("role_name", role.getRoleName());
    obj.put("role_description", role.getRoleDescription());
    obj.put("team_admin_manageable", role.isTeamAdminManageable());
    obj.put("jit_assignable", role.isJitAssignable());
    obj.put("jit_assignable_default", role.isJitAssignableDefault());
    obj.put("is_api", role.isApi());
    obj.put("ignore_team_restrictions", role.isIgnoreTeamRestrictions());

    JSONArray permissionsArray = new JSONArray();
    for (VeracodePermission permission : role.getPermissions()) {
      JSONObject permObj = new JSONObject();
      permObj.put("permission_name", permission.getPermissionName());
      JSONArray typesArray = new JSONArray(permission.getPermissionTypes());
      permObj.put("permission_types", typesArray);
      permissionsArray.put(permObj);
    }
    obj.put("permissions", permissionsArray);

    JSONArray childRolesArray = new JSONArray();
    for (VeracodeRole childRole : role.getChildRoles()) {
      JSONObject childObj = new JSONObject();
      childObj.put("role_name", childRole.getRoleName());
      childRolesArray.put(childObj);
    }
    obj.put("child_roles", childRolesArray);

    return obj;
  }

  public static class RoleImportData {
    public final String roleName;
    public final String roleDescription;
    public final boolean teamAdminManageable;
    public final boolean jitAssignable;
    public final boolean jitAssignableDefault;
    public final boolean isApi;
    public final boolean ignoreTeamRestrictions;
    private final JSONArray permissions;
    private final List<VeracodeRole> childRoles;

    public RoleImportData(JSONObject obj) throws JSONException {
      this.roleName = obj.optString("role_name", "");
      this.roleDescription = obj.optString("role_description", "");
      this.teamAdminManageable = obj.optBoolean("team_admin_manageable", false);
      this.jitAssignable = obj.optBoolean("jit_assignable", false);
      this.jitAssignableDefault = obj.optBoolean("jit_assignable_default", false);
      this.isApi = obj.optBoolean("is_api", false);
      this.ignoreTeamRestrictions = obj.optBoolean("ignore_team_restrictions", false);
      this.permissions = obj.optJSONArray("permissions");
      this.childRoles = parseChildRoles(obj.optJSONArray("child_roles"));
    }

    private static List<VeracodeRole> parseChildRoles(JSONArray childRolesArray) throws JSONException {
      if (childRolesArray == null) {
        return Collections.emptyList();
      }
      List<VeracodeRole> result = new ArrayList<>(childRolesArray.length());
      for (int i = 0; i < childRolesArray.length(); i++) {
        String childRoleName = childRolesArray.getJSONObject(i).optString("role_name", "");
        if (!childRoleName.isEmpty()) {
          result.add(new VeracodeRole(null, -1, childRoleName, "", false, false, false, false, false,
              Collections.emptyList(), Collections.emptyList()));
        }
      }
      return Collections.unmodifiableList(result);
    }

    public List<VeracodePermission> getPermissions() throws JSONException {
      List<VeracodePermission> result = new ArrayList<>();
      if (permissions != null) {
        for (int i = 0; i < permissions.length(); i++) {
          JSONObject permObj = permissions.getJSONObject(i);
          String permName = permObj.optString("permission_name", "");
          if (permName.isEmpty()) {
            throw new JSONException("Permission at index " + i + " is missing required field 'permission_name'");
          }
          JSONArray typesArray = permObj.optJSONArray("permission_types");
          List<String> types = new ArrayList<>();
          if (typesArray != null) {
            for (int j = 0; j < typesArray.length(); j++) {
              String typeStr = typesArray.optString(j);
              if (typeStr != null && !typeStr.isEmpty()) {
                types.add(typeStr);
              }
            }
          }
          result.add(new VeracodePermission(null, permName, "", true, false, false, types));
        }
      }
      return result;
    }

    public List<VeracodeRole> getChildRoles() {
      return childRoles;
    }
  }
}
