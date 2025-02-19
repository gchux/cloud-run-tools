package dev.chux.gcp.crun.rest;

import com.google.common.base.Optional;

import com.google.gson.Gson;

import spark.Request;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Strings.isNullOrEmpty;

public interface Route extends spark.Route {

  public String endpoint(final String basePath);

  public void register(final String basePath);

  public default boolean isMultiple(
    final Request request,
    final String queryParam
  ) {
    final String type = request.queryParams(queryParam);
    if (isNullOrEmpty(type)) {
      return false;
    }
    return type.equalsIgnoreCase("multi") || type.equalsIgnoreCase("batch");
  }

  public default <T> Optional<T> jsonPayload(
    final Gson gson,
    final String rawJSON,
    final Class<T> clazz
  ) throws Exception {
    return fromNullable(gson.fromJson(rawJSON, clazz));
  }

}
