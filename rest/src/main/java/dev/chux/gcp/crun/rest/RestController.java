package dev.chux.gcp.crun.rest;

import com.google.common.base.Joiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Request;
import spark.Response;

public abstract class RestController implements Route {

  private static final Logger logger = LoggerFactory.getLogger(RestController.class);

  private static final Joiner PATH_JOINER = Joiner.on('/').skipNulls();

  private String root;
  private String base;
  private String path;

  protected final void register(
    final String root,
    final String base,
    final String path
  ) {
    this.root = root;
    this.base = PATH_JOINER.join(this.root, base);
    this.path = PATH_JOINER.join(this.base, path);
  }

  public final String apiRoot() {
    return this.root;
  }

  public final String apiBase() {
    return this.base;
  }

  public final String apiPath() {
    return this.path;
  }

  protected final String appendToBase(
    final String suffix
  ) {
    return PATH_JOINER.join(
      this.apiBase(), suffix
    );
  }

  protected final String appendToPath(
    final String suffix
  ) {
    return PATH_JOINER.join(
      this.apiPath(), suffix
    );
  }

  protected final String requestMethod(
    final Request request
  ) {
    return request.requestMethod().toUpperCase();
  }

  protected final boolean isHEAD(
    final Request request
  ) {
    return requestMethod(request).equals("HEAD");
  }

}
