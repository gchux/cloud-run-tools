package dev.chux.gcp.crun.jmeter.rest;

import java.io.OutputStream;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.inject.Inject;

import com.google.common.io.ByteSource;
import com.google.common.io.MoreFiles;

import spark.Request;
import spark.Response;

import dev.chux.gcp.crun.ConfigService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

import static java.nio.file.StandardOpenOption.READ;

public class CatalogJMeterTestController extends JMeterTestController {

  private static final Logger logger = LoggerFactory.getLogger(CatalogJMeterTestController.class);

  private static final String PROPERTY_JMAAS_CATALOG = "jmaas.catalog";
  private static final String DEFAULT_JMAAS_CATALOG = "/jmaas/catalog.json";

  private final Path catalog;
  private final ByteSource source;

  @Inject
  public CatalogJMeterTestController(
    final ConfigService configService
  ) {
    this.catalog = this.catalog(configService);
    this.source = this.source(this.catalog);
  }

  private ByteSource source(
    final Path catalog
  ) {
    return MoreFiles.asByteSource(catalog, READ);
  }

  private Path catalog(
    final ConfigService configService
  ) {
    return Paths.get(
      this.catalogPath(configService)
    );
  }

  private String catalogPath(
    final ConfigService configService
  ) {
    return configService
      .getOptionalAppProp(PROPERTY_JMAAS_CATALOG)
      .or(DEFAULT_JMAAS_CATALOG);
  }

  @Override
  public void register(
    final String basePath
  ) {
    register(basePath, "catalog");
    path(apiBase(), () -> {
      get("/catalog", this);
    });
  }

  @Override
  public String endpoint(
    final String basePath
  ) {
    return "[GET] " + apiPath();
  }

  public Object handle(
    final Request request,
    final Response response
  ) throws Exception {
    response.header("Content-Type", "application/json");
    response.header("Content-Encoding", "gzip");
    final OutputStream responseOutputStream = response.raw().getOutputStream();
    this.source.copyTo(responseOutputStream);
    responseOutputStream.flush();
    return null;
  }

}
