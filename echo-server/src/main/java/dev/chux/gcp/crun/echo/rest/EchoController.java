package dev.chux.gcp.crun.echo.rest;

import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Map;

import com.google.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import com.google.gson.Gson;

import spark.Request;
import spark.Response;

import dev.chux.gcp.crun.rest.Route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Strings.emptyToNull;

import static spark.Spark.*;

public class EchoController implements Route {
  private static final Logger logger = LoggerFactory.getLogger(EchoController.class);

  public static final String NAMESPACE = RestModule.NAMESPACE + "/http/request";

  public static final String PROPERTIES_PREFIX = RestModule.PROPERTIES_PREFIX + ".echo.request";

  private final Gson gson;

  @Inject
  public EchoController(final Gson gson) {
    this.gson = gson;
  }

  public void register(final String root) {
    path(root, () -> {
      get("/request", "*/*", this);
      post("/request", "*/*", this);
      put("/request", "*/*", this);
      patch("/request", "*/*", this);
      delete("/request", "*/*", this);
      options("/request", "*/*", this);
    });
  }

  public String endpoint(final String root) {
    return "* " + root + "/request";
  }

  public Object handle(
    final Request request,
    final Response response
  ) throws Exception {
    final Optional<String> body = fromNullable(emptyToNull(request.body()));

    final ImmutableMap.Builder<String, String> headers = ImmutableMap.<String, String>builder();
    for (final String header : request.headers()) {
      headers.put(header, request.headers(header));
    }

    final ImmutableMap.Builder<String, List<String>> query = ImmutableMap.<String, List<String>>builder();
    for(final Map.Entry<String, String[]> entry : request.queryMap().toMap().entrySet()) {
      query.put(entry.getKey(), ImmutableList.<String>copyOf(entry.getValue()));
    }

    final ImmutableMap<String, Object> reply =
      ImmutableMap.<String, Object>builder()
        .put("proto", request.protocol())
        .put("method", request.requestMethod())
        .put("url", request.url())
        .put("query", query.build())
        .put("headers", headers.build())
        .put("body", body.or(""))
        .build();

    logger.info("Request[{}]", toStringHelper(request.matchedPath()).addValue(reply));

    final OutputStream stream = response.raw().getOutputStream();
    final OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
    this.gson.toJson(reply, writer);
    writer.flush();
    return null;
  }

}
