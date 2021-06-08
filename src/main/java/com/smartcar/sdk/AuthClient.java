package com.smartcar.sdk;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.smartcar.sdk.data.Auth;
import com.smartcar.sdk.data.Compatibility;
import com.smartcar.sdk.data.RequestPaging;
import com.smartcar.sdk.data.ResponsePaging;
import com.smartcar.sdk.data.SmartcarResponse;
import com.smartcar.sdk.data.VehicleIds;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Smartcar OAuth 2.0 Authentication Client */
public class AuthClient extends ApiClient {
  /** Custom deserializer for Auth data from the OAuth endpoint. */
  private class AuthDeserializer implements JsonDeserializer<Auth> {
    /**
     * Deserializes the OAuth auth endpoint JSON into a new Auth object.
     *
     * @param json the Json data being deserialized
     * @param typeOfT the type of the Object to deserialize to
     * @param context the deserialization context
     * @return the newly created Auth object
     */
    public Auth deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
      JsonObject jsonObject = json.getAsJsonObject();

      // Get timestamp for expiration.
      Calendar expiration = Calendar.getInstance();
      expiration.add(Calendar.SECOND, jsonObject.get("expires_in").getAsInt());

      Calendar refreshExpiration = Calendar.getInstance();
      refreshExpiration.add(Calendar.DAY_OF_YEAR, 60);

      return new Auth(
          jsonObject.get("access_token").getAsString(),
          jsonObject.get("refresh_token").getAsString(),
          expiration.getTime(),
          refreshExpiration.getTime());
    }
  }

  private static final String URL_AUTHORIZE = "https://connect.smartcar.com/oauth/authorize";
  private static final String URL_ACCESS_TOKEN = "https://auth.smartcar.com/oauth/token";

  private String clientId;
  private String basicAuthorization;
  private String redirectUri;
  private String[] scope;
  private boolean testMode;
  public String urlAuthorize = AuthClient.URL_AUTHORIZE;
  public String urlAccessToken = AuthClient.URL_ACCESS_TOKEN;

  /**
   * Retrieves the user ID of the user authenticated with the specified access token.
   *
   * @param accessToken a valid access token
   * @return the corresponding user ID
   * @throws SmartcarException if the request is unsuccessful
   */
  public static String getUserId(String accessToken) throws SmartcarException {
    // Build Request
    Request request =
        new Request.Builder()
            .url(HttpUrl.parse(AuthClient.getApiUrl() + "/user"))
            .header("Authorization", "Bearer " + accessToken)
            .addHeader("User-Agent", AuthClient.USER_AGENT)
            .build();

    // Execute Request
    Response response = AuthClient.execute(request);

    // Parse Response
    JsonObject json;

    try {
      json = new Gson().fromJson(response.body().string(), JsonObject.class);
      return json.get("id").getAsString();
    } catch (IOException ex) {
      throw new SmartcarException(ex.getMessage());
    }
  }

  /**
   * Retrieves all vehicle IDs associated with the authenticated user.
   *
   * @param accessToken a valid access token
   * @param paging paging parameters
   * @return the requested vehicle IDs
   * @throws SmartcarException if the request is unsuccessful
   */
  public static SmartcarResponse<VehicleIds> getVehicleIds(String accessToken, RequestPaging paging)
      throws SmartcarException {
    // Build Request
    HttpUrl.Builder urlBuilder = HttpUrl.parse(AuthClient.getApiUrl() + "/vehicles").newBuilder();

    if (paging != null) {
      urlBuilder
          .addQueryParameter("limit", String.valueOf(paging.getLimit()))
          .addQueryParameter("offset", String.valueOf(paging.getOffset()));
    }

    HttpUrl url = urlBuilder.build();
    Request request =
        new Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + accessToken)
            .addHeader("User-Agent", AuthClient.USER_AGENT)
            .build();

    // Execute Request
    Response response = AuthClient.execute(request);

    // Parse Response
    JsonObject json = null;

    try {
      json = new Gson().fromJson(response.body().string(), JsonObject.class);
    } catch (IOException ex) {
      throw new SmartcarException(ex.getMessage());
    }

    JsonObject jsonPaging = json.get("paging").getAsJsonObject();
    ResponsePaging responsePaging =
        new ResponsePaging(jsonPaging.get("count").getAsInt(), jsonPaging.get("offset").getAsInt());
    JsonArray vehicles = json.get("vehicles").getAsJsonArray();
    int count = vehicles.size();
    String[] data = new String[count];

    for (int i = 0; i < count; i++) {
      data[i] = vehicles.get(i).getAsString();
    }

    return new SmartcarResponse<VehicleIds>(new VehicleIds(data), responsePaging);
  }

  /**
   * Retrieves all vehicle IDs associated with the authenticated user.
   *
   * @param accessToken a valid access token
   * @return the requested vehicle IDs
   * @throws SmartcarException if the request is unsuccessful
   */
  public static SmartcarResponse<VehicleIds> getVehicleIds(String accessToken)
      throws SmartcarException {
    return AuthClient.getVehicleIds(accessToken, null);
  }

  /**
   * Convenience method for determining if an auth token expiration has passed.
   *
   * @param expiration the expiration date of the token
   * @return whether or not the token has expired
   */
  public static boolean isExpired(Date expiration) {
    return !expiration.after(new Date());
  }

  /**
   * Initializes a new AuthClient.
   *
   * @param clientId the application client ID
   * @param clientSecret the application client secret
   * @param redirectUri the registered redirect URI for the application
   * @param testMode launch the Smartcar auth flow in test mode
   */
  public AuthClient(
      String clientId, String clientSecret, String redirectUri, boolean testMode) {
    this.clientId = clientId;
    this.basicAuthorization = Credentials.basic(clientId, clientSecret);
    this.redirectUri = redirectUri;
    this.testMode = testMode;

    AuthClient.gson.registerTypeAdapter(Auth.class, new AuthDeserializer());
  }

  /**
   * Initializes a new AuthClient.
   *
   * @param clientId the application client ID
   * @param clientSecret the application client secret
   * @param redirectUri the registered redirect URI for the application
   */
  public AuthClient(String clientId, String clientSecret, String redirectUri) {
    this(clientId, clientSecret, redirectUri, false);
  }

  /**
   * Creates an AuthUrlBuilder
   *
   * @param scope the permission scope requested
   * @return returns an instance of AuthUrlBuilder
   */
  public AuthUrlBuilder authUrlBuilder(String[] scope) {
    return new AuthUrlBuilder(scope);
  }

  /**
   * Executes an Auth API request.
   *
   * @param requestBody the request body to be included
   * @return the parsed response
   * @throws SmartcarException if the API request fails
   */
  private Auth call(RequestBody requestBody) throws SmartcarException {
    Request request =
        new Request.Builder()
            .url(this.urlAccessToken)
            .header("Authorization", this.basicAuthorization)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("User-Agent", AuthClient.USER_AGENT)
            .post(requestBody)
            .build();

    return AuthClient.execute(request, Auth.class).getData();
  }

  /**
   * A builder for creating Authorization URLs. Access through {@link AuthClient#authUrlBuilder()}.
   */
  public class AuthUrlBuilder {
    private HttpUrl.Builder urlBuilder;

    public AuthUrlBuilder(String[] scope) {
      urlBuilder =
          HttpUrl.parse(AuthClient.this.urlAuthorize)
              .newBuilder()
              .addQueryParameter("response_type", "code")
              .addQueryParameter("client_id", AuthClient.this.clientId)
              .addQueryParameter("redirect_uri", AuthClient.this.redirectUri)
              .addQueryParameter("mode", AuthClient.this.testMode ? "test" : "live")
              .addQueryParameter("scope", Utils.join(scope, " "));
    }

    public AuthUrlBuilder setState(String state) {
      if (state != "") {
        urlBuilder.addQueryParameter("state", state);
      }
      return this;
    }

    public AuthUrlBuilder setApprovalPrompt(boolean approvalPrompt) {
      urlBuilder.addQueryParameter("approval_prompt", approvalPrompt ? "force" : "auto");
      return this;
    }

    public AuthUrlBuilder setMakeBypass(String make) {
      urlBuilder.addQueryParameter("make", make);
      return this;
    }

    public AuthUrlBuilder setSingleSelect(boolean singleSelect) {
      urlBuilder.addQueryParameter("single_select", Boolean.toString(singleSelect));
      return this;
    }

    public AuthUrlBuilder setSingleSelectVin(String vin) {
      urlBuilder.addQueryParameter("single_select_vin", vin);
      return this;
    }

    public AuthUrlBuilder setFlags(String[] flags) {
      urlBuilder.addQueryParameter("flags", Utils.join(flags, " "));
      return this;
    }

    public String build() {
      return urlBuilder.build().toString();
    }
  }

  /**
   * Exchanges an authorization code for an access token.
   *
   * @param code the authorization code
   * @return the requested access token
   * @throws SmartcarException when the request is unsuccessful
   */
  public Auth exchangeCode(String code) throws SmartcarException {
    RequestBody requestBody =
        new FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", this.redirectUri)
            .build();

    return this.call(requestBody);
  }

  /**
   * Exchanges a refresh token for a new access token.
   *
   * @param refreshToken the refresh token
   * @return the requested access token
   * @throws SmartcarException when the request is unsuccessful
   */
  public Auth exchangeRefreshToken(String refreshToken) throws SmartcarException {
    RequestBody requestBody =
        new FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .build();

    return this.call(requestBody);
  }

  /**
   * Determine if a vehicle is compatible with the Smartcar API and the provided permissions. A
   * compatible vehicle is a vehicle that:
   *
   * <ol>
   *   <li>has the hardware required for internet connectivity,
   *   <li>belongs to the makes and models Smartcar supports, and
   *   <li>supports the permissions.
   * </ol>
   *
   * @param vin the VIN (Vehicle Identification Number) of the vehicle.
   * @param scope An array of permissions. The valid permissions are found in the API Reference.
   * @return false if the vehicle is not compatible. true if the vehicle is likely compatible.
   * @throws SmartcarException when the request is unsuccessful
   */
  public boolean isCompatible(String vin, String[] scope) throws SmartcarException {
    return isCompatible(vin, scope, "US");
  }

  /**
   * Determine if a vehicle is compatible with the Smartcar API and the provided permissions for the
   * specified country. A compatible vehicle is a vehicle that:
   *
   * <ol>
   *   <li>has the hardware required for internet connectivity,
   *   <li>belongs to the makes and models Smartcar supports, and
   *   <li>supports the permissions.
   * </ol>
   *
   * @param vin the VIN (Vehicle Identification Number) of the vehicle.
   * @param scope An array of permissions. The valid permissions are found in the API Reference.
   * @param country An optional country code according to [ISO 3166-1
   *     alpha-2](https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2)
   * @return false if the vehicle is not compatible in the specified country. true if the vehicle is
   *     likely compatible.
   * @throws SmartcarException when the request is unsuccessful
   */
  public boolean isCompatible(String vin, String[] scope, String country) throws SmartcarException {
    String apiUrl = this.getApiUrl();
    HttpUrl url =
        HttpUrl.parse(apiUrl)
            .newBuilder()
            .addPathSegment("compatibility")
            .addQueryParameter("vin", vin)
            .addQueryParameter("scope", String.join(" ", scope))
            .addQueryParameter("country", country)
            .build();

    Request request =
        new Request.Builder()
            .url(url)
            .header("Authorization", this.basicAuthorization)
            .addHeader("User-Agent", AuthClient.USER_AGENT)
            .get()
            .build();

    return AuthClient.execute(request, Compatibility.class).getData().getCompatible();
  }
}
