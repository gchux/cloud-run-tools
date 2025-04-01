package dev.chux.gcp.crun.rest;

import java.util.Map;

import javax.servlet.ServletOutputStream;

import com.google.inject.Inject;

import com.google.common.base.Joiner;

import spark.Request;
import spark.Response;

import dev.chux.gcp.crun.ConfigService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

public abstract class RestController implements Route {

  private static final Logger logger = LoggerFactory.getLogger(RestController.class);

  private static final Joiner PATH_JOINER = Joiner.on('/').skipNulls();

  protected String root;

  protected String base;
  
  protected String path;

  public void register(
    final String root,
    final String base,
    final String path
  ) {
    this.root = root;
    this.base = PATH_JOINER.join(this.root, base);
    this.path = PATH_JOINER.join(this.base, path);
  }

  protected String apiRoot() {
    return this.root;
  }

  protected String apiBase() {
    return this.base;
  }

  protected String apiPath() {
    return this.path;
  }

  protected String appendToPath(
    final String suffix
  ) {
    return PATH_JOINER.join(
      this.apiPath(), suffix
    );
  }

}

