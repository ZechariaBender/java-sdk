package com.smartcar.sdk;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;

import okhttp3.Headers;
import okhttp3.Response;
import okhttp3.ResponseBody;

import javax.json.Json;

/** Thrown when the Smartcar API library encounters a problem. */
public class SmartcarException extends java.lang.Exception {

  private final int statusCode;
  private final String type;
  private final String code;
  private final String description;
  // TODO change to JsonObject{ type, url }
  private final String resolution;
  private final JsonArray detail;
  private final String docURL;
  private final String requestId;

  public static class Builder {
    private int statusCode;
    private String type;
    private String code;
    private String description;
    private String resolution;
    private JsonArray detail;
    private String docURL;
    private String requestId;

    public Builder() {
      this.statusCode = 0;
      this.type = "";
      this.code = null;
      this.description = "";
      this.resolution = null;
      this.detail = null;
      this.docURL = "";
      this.requestId = "";
    }

    public Builder statusCode(int statusCode) {
      this.statusCode = statusCode;
      return this;
    }

    public Builder type(String type) {
      this.type = type;
      return this;
    }

    public Builder code(String code) {
      this.code = code;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder resolution(String resolution) {
      this.resolution = resolution;
      return this;
    }

    public Builder detail(JsonArray detail) {
      this.detail = detail;
      return this;
    }

    public Builder docURL(String docURL) {
      this.docURL = docURL;
      return this;
    }

    public Builder requestId(String requestId) {
      this.requestId = requestId;
      return this;
    }

    public SmartcarException build() { return new SmartcarException(this); }
  }

  private SmartcarException(Builder builder) {
    this.statusCode = builder.statusCode;
    this.type = builder.type;
    this.code = builder.code;
    this.description = builder.description;
    this.resolution = builder.resolution;
    this.detail = builder.detail;
    this.docURL = builder.docURL;
    this.requestId = builder.requestId;
  }

  public static SmartcarException Factory(final int statusCode, JsonObject headers, JsonObject body) {
    String requestId = headers.get("SC-Request-Id").getAsString();
    Builder builder = new SmartcarException.Builder().statusCode(statusCode).requestId(requestId);

    JsonElement contentType = headers.get("Content-Type");
    if (contentType != null && !contentType.getAsString().contains("application/json")) {
      return builder.description(body.toString()).build();
    }

    JsonObject bodyJson = new Gson().fromJson(body.toString(), JsonObject.class);
    if (bodyJson.has("error")) {
      builder
              .type(bodyJson.get("error").getAsString())
              .description(bodyJson.get("message").getAsString());
      if (bodyJson.has("code")) {
        builder.code(bodyJson.get("code").getAsString());
      }
      return builder.build();
    } else if (bodyJson.has("type")) {
      builder
              .type(bodyJson.get("type").getAsString())
              .description(bodyJson.get("description").getAsString())
              .docURL(bodyJson.get("docURL").getAsString());

      if (!bodyJson.get("code").isJsonNull()) {
        builder.code(bodyJson.get("code").getAsString());
      } else {
        builder.code(null);
      }

      if (!bodyJson.get("resolution").isJsonNull()) {
        builder.resolution(bodyJson.get("resolution").getAsString());
      } else {
        builder.resolution(null);
      }

      if (bodyJson.has("detail")) {
        JsonArray detailJson = bodyJson.get("detail").getAsJsonArray();
        builder.detail(detailJson);
      }

      return builder.build();
    }

    return builder
            .requestId(requestId)
            .description(body.toString())
            .type("SDK_ERROR")
            .build();
  }

  public static SmartcarException Factory(final int statusCode, Headers headers, ResponseBody body) {
    JsonObject headerJson = new JsonObject();
    for (String header: headers.names()) {
      headerJson.addProperty(header, headers.get(header));
    }

    JsonObject bodyJson = null;
    String bodyString = null;
    try {
      bodyString = body.string();
    } catch (IOException e) {
      return new SmartcarException.Builder()
              .statusCode(statusCode)
              .description("Unable to get request body")
              .requestId(headers.get("SC-Request-Id"))
              .type("SDK_ERROR")
              .build();
    }
    try {
      bodyJson = new Gson().fromJson(bodyString, JsonObject.class);
    } catch (Exception e) {
      // Handles non 200 invalid JSON errors
      return new SmartcarException.Builder()
              .statusCode(statusCode)
              .description(bodyString)
              .requestId(headers.get("SC-Request-Id"))
              .type("SDK_ERROR")
              .build();
    }

    return SmartcarException.Factory(statusCode, headerJson, bodyJson);
  }

  /**
   * Returns the error message
   *
   * @return message
   */
  public String getMessage() {
    if (this.type != null) {
      return this.type + ":" + this.code + " - " + this.description;
    }
    return this.description;
  }

  public int getStatusCode() { return this.statusCode; }

  public String getRequestId() { return this.requestId; }

  /**
   * Returns the error type associated with the SmartcarExceptionV2.
   *
   * @return the error type
   */
  public String getType() {
    return this.type;
  }

  public String getCode() { return this.code; }

  /**
   * Returns the description associated with the exception.
   *
   * @return the description
   */
  public String getDescription() {
    return this.description;
  }

  /**
   * Returns the resolution message associated with the exception.
   *
   * @return the resolution message
   */
  public String getResolution() {
    return this.resolution;
  }

  /**
   * Returns the documentation URL associated with the exception.
   *
   * @return the documentation URL
   */
  public String getDocURL() {
    return this.docURL;
  }

  /**
   * Returns the error details if available for this exception.
   *
   * @return the error details
   */
  public JsonArray getDetail() {
    return this.detail;
  }

}
