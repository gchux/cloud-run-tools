package dev.chux.gcp.crun;

import static spark.Spark.*;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.UUID;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import com.google.common.base.Optional;

import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;

import org.apache.commons.cli.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import dev.chux.gcp.crun.http.HttpServer;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

public class App {
  private static final Logger logger = LoggerFactory.getLogger(App.class);

  private static final String PROPERTIES_FILE_OPTION = "config";
  private static final String DEFAULT_PROPERTIES_FILE = "/process-runner.properties";

  private static final Options OPTIONS = new Options();

  static {
    OPTIONS.addOption("c", PROPERTIES_FILE_OPTION, true, "full path to properties file");
  }

  private final Optional<Module> module;

  public static App newApp() {
    return new App(absent());
  }

  public static App newApp(@Nullable final Module module) {
    return new App(fromNullable(module));
  }

  private App(@Nullable final Optional<Module> module) {
    this.module = module;
  }

  public void run(final String[] args) {
    final CommandLineParser parser = new DefaultParser();
    final Optional<CommandLine> maybeCmdln = command(parser, args);

    if (!maybeCmdln.isPresent()) {
      // error parsing command line
      System.exit(1);
    }

    final CommandLine cmdln = maybeCmdln.get();

    final String propertiesFile = getPropertiesFile(cmdln);
    logger.info("using properties file: {}", propertiesFile);
    
    final Injector injector = createInjector(propertiesFile);
    
    start(injector);
  }

  private final Optional<LifecycleManager> getLifecycleManager(@NonNull @CheckForNull final Injector injector) {
    try {
      final LifecycleManager manager = checkNotNull(injector).getInstance(LifecycleManager.class);
      return fromNullable(manager);
    } catch(final Exception ex) {
      logger.error("nothing provides LifecycleManager", ex);
    }
    return absent();
  }

  private final @NonNull Optional<HttpServer> getHttpServer(@NonNull @CheckForNull final Injector injector) {
    try {
      final HttpServer httpServer = checkNotNull(injector).getInstance(HttpServer.class);
      return fromNullable(httpServer);
    } catch(final Exception ex) {
      logger.error("nothing provides HttpServer", ex);
    }
    return absent();
  }

  private final void start(@NonNull final Injector injector) {
    final Optional<LifecycleManager> maybeManager = getLifecycleManager(injector);

    if (!maybeManager.isPresent()) {
      logger.error("LifecycleManager is unavailable");
      return;
    }

    final LifecycleManager manager = maybeManager.get();

    try {
      manager.start();
      startHttpServer(injector);
    } catch(Exception ex) {
      logger.error("failed to start HttpServer", ex);
      return;
    }

    manager.close();
  }

  private final void startHttpServer(@NonNull @CheckForNull final Injector injector) {
    final Optional<HttpServer> maybeHttpServer = getHttpServer(checkNotNull(injector));

    if (!maybeHttpServer.isPresent()) {
      // nothing provides HTTP server
      System.exit(2);
    }

    // this is blocking
    maybeHttpServer.get().start();
  }

  private final AppModule newAppModule(@NonNull @CheckForNull final String propertiesFile) {
    checkArgument(!isNullOrEmpty(propertiesFile), "missing properties file");
    return new AppModule(propertiesFile, this.module);
  }

  private final @NonNull Injector createInjector(@NonNull @CheckForNull final String propertiesFile) {
    final AppModule appModule = newAppModule(propertiesFile);
    return LifecycleInjector.builder().withBootstrapModule(appModule).withModules(appModule).build().createInjector();
  }

  private final @NonNull Optional<CommandLine> command(@NonNull @CheckForNull final CommandLineParser parser, final String[] args) {
    try {
      return fromNullable(checkNotNull(parser).parse(OPTIONS, args));
    } catch (final ParseException ex) {
      logger.error("failed to parse command line", ex);
    }
    return absent();
  }

  private final @NonNull String getOptionOrDefault(@NonNull final CommandLine cmdln,
    @NonNull @CheckForNull final String optionName, @NonNull @CheckForNull final String defaultValue) {
    checkArgument(!isNullOrEmpty(optionName), "missing option name");
    checkArgument(!isNullOrEmpty(defaultValue), "missing default value");
    return cmdln.getOptionValue(optionName, defaultValue);
  }

  private final @NonNull String getPropertiesFile(@NonNull final CommandLine cmdln) {
    return getOptionOrDefault(cmdln, PROPERTIES_FILE_OPTION, DEFAULT_PROPERTIES_FILE);
  }

}
