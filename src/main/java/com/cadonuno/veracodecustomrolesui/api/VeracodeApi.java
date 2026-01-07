package com.cadonuno.veracodecustomrolesui.api;

import com.cadonuno.veracodecustomrolesui.MessageHandler;
import com.cadonuno.veracodecustomrolesui.models.VeracodePermission;
import com.cadonuno.veracodecustomrolesui.models.VeracodeRole;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

import java.util.*;

public class VeracodeApi {

  public VeracodeApi() {
  }

  public static boolean validateCredentials(ApiCredentials apiCredentials) {
    try {
      return ApiCaller.runApi(apiCredentials, "/api/authn/v2/roles?size=1", "GET", null).isPresent();
    } catch (Exception ignored) {
      return false;
    }
  }

  public static Optional<ApiResults> tryDeleteRole(ApiCredentials apiCredentials, String roleId) {
    return ApiCaller.runApi(apiCredentials, "/api/authn/v2/roles/" + roleId,
        "DELETE", null);
  }

  public static Optional<ApiResults> trySaveCustomRole(ApiCredentials apiCredentials, VeracodeRole veracodeRole) {
    String fullUrl = "/api/authn/v2/roles" + (veracodeRole.getRoleId() != null ? "/" + veracodeRole.getRoleId() : "");
    String httpMethod = veracodeRole.getRoleId() != null ? "PUT" : "POST";
    return ApiCaller.runApi(apiCredentials, fullUrl,
        httpMethod, veracodeRole.toJsonParameter());
  }

  public static Set<VeracodePermission> getAllPermissions(ApiCredentials previousCredentials) {
    Set<VeracodePermission> foundPermissions = new TreeSet<>();
    int currentPage = 0;
    Optional<ApiResults> apiResults;
    do {
      apiResults = ApiCaller.runApi(previousCredentials, "/api/authn/v2/permissions?page=" + currentPage,
              "GET", null)
          .filter(ApiResults::hasResponse);
      foundPermissions.addAll(apiResults
          .map((results) -> getPermissionsFromPayload(results.getApiResponse()))
          .orElse(Collections.emptyList()));
      currentPage++;
    } while (hasMorePages(apiResults.orElse(null), currentPage));

    return foundPermissions;
  }

  public static Set<VeracodeRole> getAllRoles(ApiCredentials previousCredentials) {
    Set<VeracodeRole> foundRoles = new TreeSet<>();
    int currentPage = 0;
    Optional<ApiResults> apiResults;
    do {
      apiResults = ApiCaller.runApi(previousCredentials, "/api/authn/v2/roles?page=" + currentPage,
              "GET", null)
          .filter(ApiResults::hasResponse);
      foundRoles.addAll(apiResults
          .map((results) -> getRolesFromPayload(results.getApiResponse()))
          .orElse(Collections.emptyList()));
      currentPage++;
    } while (hasMorePages(apiResults.orElse(null), currentPage));

    return foundRoles;
  }

  private static boolean hasMorePages(ApiResults apiResults, int currentPage) {
    if (apiResults == null) {
      return false;
    }
    return Optional.of(apiResults)
        .map(ApiResults::getApiResponse)
        .flatMap(response -> tryGetElementFromJsonObject(response, "page"))
        .filter(result -> result instanceof JSONObject)
        .map(VeracodeApi::mapToJsonObject)
        .map(pageNode -> getTotalPagesFromNode(pageNode) > currentPage)
        .orElse(false);
  }

  private static int getTotalPagesFromNode(JSONObject pageNode) {
    return tryGetElementAsInteger(pageNode, "total_pages").orElse(0);
  }

  private static List<VeracodePermission> getPermissionsFromPayload(JSONObject apiResponse) {
    return Optional.of(apiResponse)
        .flatMap(VeracodeApi::getEmbeddedNode)
        .flatMap(embeddedNode -> getArrayNode(embeddedNode, "permissions"))
        .map(VeracodeApi::getPermissionsFromNode)
        .orElse(Collections.emptyList());
  }

  private static List<VeracodeRole> getRolesFromPayload(JSONObject apiResponse) {
    return Optional.of(apiResponse)
        .flatMap(VeracodeApi::getEmbeddedNode)
        .flatMap(embeddedNode -> getArrayNode(embeddedNode, "roles"))
        .map(VeracodeApi::getRolesFromNode)
        .orElse(Collections.emptyList());
  }

  public static List<VeracodeRole> getRolesFromNode(JSONArray allRoles) {
    List<VeracodeRole> foundRoles = new ArrayList<>();

    for (int currentIndex = 0; currentIndex < allRoles.length(); ++currentIndex) {
      tryGetJsonObjectAtJsonArrayIndex(allRoles, currentIndex)
          .map(VeracodeRole::fromJsonObject)
          .ifPresent(foundRoles::add);
    }

    return foundRoles;
  }

  public static List<VeracodePermission> getPermissionsFromNode(JSONArray allPermissions) {
    List<VeracodePermission> foundPermissions = new ArrayList<>();

    for (int currentIndex = 0; currentIndex < allPermissions.length(); ++currentIndex) {
      tryGetJsonObjectAtJsonArrayIndex(allPermissions, currentIndex)
          .map(VeracodePermission::fromJsonObject)
          .ifPresent(foundPermissions::add);
    }

    return foundPermissions;
  }


  private static Optional<JSONObject> getEmbeddedNode(JSONObject baseNode) {
    return tryGetElementFromJsonObject(baseNode, "_embedded")
        .filter(result -> result instanceof JSONObject)
        .map(VeracodeApi::mapToJsonObject);
  }

  private static Optional<JSONObject> tryGetJsonObjectAtJsonArrayIndex(JSONArray elementArray, int currentIndex) {
    try {
      Object element = elementArray.get(currentIndex);
      if (element instanceof JSONObject) {
        return Optional.of((JSONObject) element);
      }
    } catch (JSONException e) {
      MessageHandler.showError(e);
    }

    return Optional.empty();
  }

  private static Optional<Object> getJavaObjectAtJsonArrayIndex(JSONArray elementArray, int currentIndex) {
    return Optional.of(elementArray.get(currentIndex));
  }

  private static Optional<Object> tryGetElementFromJsonObject(JSONObject jsonObject, String elementToGet) {
    try {
      return Optional.of(jsonObject.get(elementToGet));
    } catch (JSONException e) {
      return Optional.empty();
    }
  }

  private static JSONObject mapToJsonObject(Object jsonResult) {
    return (JSONObject) jsonResult;
  }

  private static JSONArray mapToJsonArray(Object jsonResult) {
    return (JSONArray) jsonResult;
  }

  public static Optional<String> tryGetElementAsString(JSONObject jsonObject, String elementToGet) {
    return tryGetElementFromJsonObject(jsonObject, elementToGet)
        .filter(result -> result instanceof String)
        .map(result -> (String) result);
  }

  public static Optional<Integer> tryGetElementAsInteger(JSONObject jsonObject, String elementToGet) {
    return tryGetElementFromJsonObject(jsonObject, elementToGet)
        .filter(result -> result instanceof Integer)
        .map(result -> (Integer) result);
  }

  public static Optional<Boolean> tryGetElementAsBoolean(JSONObject jsonObject, String elementToGet) {
    return tryGetElementFromJsonObject(jsonObject, elementToGet)
        .filter(result -> result instanceof Boolean)
        .map(result -> (Boolean) result);
  }

  public static Optional<List<String>> tryGetElementAsStringList(JSONObject jsonObject, String elementToGet) {
    return getArrayNode(jsonObject, elementToGet)
        .map(VeracodeApi::convertToListOfString);
  }

  private static List<String> convertToListOfString(JSONArray objects) {
    List<String> elements = new ArrayList<>();
    for (int currentIndex = 0; currentIndex < objects.length(); ++currentIndex) {
      getJavaObjectAtJsonArrayIndex(objects, currentIndex)
          .filter(result -> result instanceof String)
          .map(result -> (String) result)
          .ifPresent(elements::add);
    }
    return elements;
  }

  public static Optional<JSONArray> getArrayNode(JSONObject embeddedNode, String nodeToGet) {
    return tryGetElementFromJsonObject(embeddedNode, nodeToGet)
        .filter(result -> result instanceof JSONArray)
        .map(VeracodeApi::mapToJsonArray);
  }
}
