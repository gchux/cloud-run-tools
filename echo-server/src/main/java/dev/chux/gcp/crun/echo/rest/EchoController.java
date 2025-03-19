package dev.chux.gcp.crun.echo.rest;

import java.util.List;
import java.util.Map;

import com.google.inject.Inject;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

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
  public static final String PROPERTY_ALLOWED_RUNTIMES = PROPERTIES_PREFIX + ".echo.request";

  @Inject
  public EchoController() {}

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
    final String body = request.body();

    final ImmutableMap.Builder<String, String> headers = ImmutableMap.<String, String>builder();
    for (final String header : request.headers()) {
      headers.put(header, request.headers(header));
    }

    final ImmutableMap.Builder<String, List<String>> query = ImmutableMap.<String, List<String>>builder();
    for(final Map.Entry<String, String[]> entry : request.queryMap().toMap().entrySet()) {
      query.put(entry.getKey(), ImmutableList.<String>copyOf(entry.getValue()));
    }

    logger.info(
      toStringHelper(request.matchedPath())
      .add("protocol", request.protocol())
      .add("method", request.requestMethod())
      .add("url", request.url())
      .add("query", query.build())
      .add("headers", headers.build())
      .addValue(fromNullable(emptyToNull(body)))
      .toString()
    );
    return body;
  }

}
