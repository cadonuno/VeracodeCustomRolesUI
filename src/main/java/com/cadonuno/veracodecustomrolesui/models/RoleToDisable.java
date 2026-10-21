package com.cadonuno.veracodecustomrolesui.models;

import org.apache.sling.commons.json.JSONObject;

import static com.cadonuno.veracodecustomrolesui.api.VeracodeApi.tryGetElementAsString;

@SuppressWarnings("deprecation")
public class RoleToDisable implements Comparable<RoleToDisable> {
    private final String roleName;
    private final String roleDescription;
    private final Boolean isEnabled;
    private final String roleId;

    public static RoleToDisable build(VeracodeRole veracodeRole) {
        return new RoleToDisable(veracodeRole.getRoleName(), veracodeRole.getRoleDescription(),
                veracodeRole.getRoleId(),true);
    }

    public static RoleToDisable build(JSONObject roleNode) {
        return new RoleToDisable(tryGetElementAsString(roleNode, "role_name").orElse(""),
                tryGetElementAsString(roleNode, "role_description").orElse(""),
                tryGetElementAsString(roleNode, "role_id").orElse(""),
                false);
    }

    private RoleToDisable(String roleName, String roleDescription, String roleId, Boolean isEnabled) {
        this.roleName = roleName;
        this.roleDescription = roleDescription;
        this.roleId = roleId;
        this.isEnabled = isEnabled;
    }

    public static RoleToDisable fromDisableAction(RoleToDisable previousRole) {
        return new RoleToDisable(previousRole.roleName, previousRole.roleDescription,
                previousRole.roleId, !previousRole.isEnabled);
    }

    public Boolean isEnabled() {
        return isEnabled;
    }

    public String getRoleId() {
        return roleId;
    }

    public String getRoleDescription() {
        return roleDescription;
    }

    public String getRoleName() {
        return roleName;
    }

    @Override
    public int compareTo(RoleToDisable other) {
        return roleDescription.toLowerCase().compareTo(other.roleDescription.toLowerCase());
    }

    @Override
    public String toString() {
        return roleDescription;
    }
}
