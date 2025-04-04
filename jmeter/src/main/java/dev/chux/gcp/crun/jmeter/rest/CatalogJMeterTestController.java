package dev.chux.gcp.crun.jmeter.rest;

import java.io.OutputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.inject.Inject;

import com.google.common.base.Optional;
import com.google.common.io.ByteSource;
import com.google.common.io.MoreFiles;

import spark.Request;
import spark.Response;

import dev.chux.gcp.crun.ConfigService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

import static java.nio.file.StandardOpenOption.READ;
import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;

public class CatalogJMeterTestController extends JMeterTestController {

  private static final Logger logger = LoggerFactory.getLogger(CatalogJMeterTestController.class);

  private static final String PROPERTY_JMAAS_CATALOGS = "jmaas.catalogs";
  private static final String DEFAULT_JMAAS_CATALOGS = "/jmaas/catalogs";
  private static final String CATALOG_EXTENSION = ".json.gz";

  private final Path catalogs;

  @Inject
  public CatalogJMeterTestController(
    final ConfigService configService
  ) {
    this.catalogs = this.catalogs(configService);
  }

  private Path catalogs(
    final ConfigService configService
  ) {
    return Paths.get(
      this.catalogsPath(configService)
    );
  }

  private String catalogsPath(
    final ConfigService configService
  ) {
    return configService
      .getOptionalAppProp(
        PROPERTY_JMAAS_CATALOGS
      ).or(DEFAULT_JMAAS_CATALOGS);
  }

  @Override
  public void register(
    final String basePath
  ) {
    register(basePath, "catalog");
    path(apiBase(), () -> {
      get("/catalog/:name", this);
      get("/catalog", this);
    });
  }

  @Override
  public String endpoint(
    final String basePath
  ) {
    return "[GET] " + apiPath();
  }

  private String catalogName(
    final Request request
  ) {
    return name(request).or("default") + CATALOG_EXTENSION;
  }

  private Path catalogPath(
    final Request request
  ) {
    return this.catalogs.resolve(
      this.catalogName(request)
    );
  }

  private Optional<
    ByteSource
  > source(
    final Request request
  ) {
    final Path catalog = this.catalogPath(request);
    if ( Files.isReadable(catalog) ) {
      return fromNullable(
        MoreFiles.asByteSource(catalog, READ)
      );
    }
    return absent();
  }

  public Object handle(
    final Request request,
    final Response response
  ) throws Exception {
    final Optional<ByteSource> source = this.source(request);

    if ( !source.isPresent() ) {
      halt(404, "catalog is unavailable");
      return null;
    }

    response.header("Content-Type", "application/json");
    response.header("Content-Encoding", "gzip");
    final OutputStream responseOutputStream = response.raw().getOutputStream();
    source.get().copyTo(responseOutputStream);
    responseOutputStream.flush();
    return null;
  }

}
