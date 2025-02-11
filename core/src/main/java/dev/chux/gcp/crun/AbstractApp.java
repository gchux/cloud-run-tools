package dev.chux.gcp.crun;

import java.util.function.Consumer;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;

import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;

import org.apache.commons.cli.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import dev.chux.gcp.crun.http.HttpModule;
import dev.chux.gcp.crun.http.HttpServer;
import dev.chux.gcp.crun.rest.RestModule;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.base.Strings.isNullOrEmpty;

abstract class AbstractApp implements Supplier<Optional<Module>> {
  private static final Logger logger = LoggerFactory.getLogger(App.class);

  private static final String PROPERTIES_FILE_OPTION = "config";
  private static final String DEFAULT_PROPERTIES_FILE = "/process-runner.properties";

  private static final Options OPTIONS = new Options();

  static {
    OPTIONS.addOption("c", PROPERTIES_FILE_OPTION, true, "full path to properties file");
  }

  private final Optional<Module> module;

  protected AbstractApp(@CheckForNull final Module coreModule, final Optional<Module> appModule) {
    if(appModule.isPresent()) {
      this.module = Optional.of(Modules.combine(checkNotNull(coreModule), appModule.get()));
    } else {
      this.module = Optional.of(coreModule);
    }
  }

  protected AbstractApp(final Optional<Module> appModule) {
    this.module = appModule;
  }

  public abstract void run(final Consumer<Injector> action, final String[] args);

  public final Optional<Module> get() {
    return this.module;
  }

  private Injector initialize(final String[] args) {
    final CommandLineParser parser = new DefaultParser();
    final Optional<CommandLine> maybeCmdln = command(parser, args);

    if (!maybeCmdln.isPresent()) {
      // error parsing command line
      System.exit(1);
    }

    final CommandLine cmdln = maybeCmdln.get();

    final String propertiesFile = getPropertiesFile(cmdln);
    logger.info("using properties file: {}", propertiesFile);
    
    return createInjector(propertiesFile);
  }

  protected final Optional<LifecycleManager> getLifecycleManager(@NonNull @CheckForNull final Injector injector) {
    try {
      final LifecycleManager manager = checkNotNull(injector).getInstance(LifecycleManager.class);
      return fromNullable(manager);
    } catch(final Exception ex) {
      logger.error("nothing provides LifecycleManager: {}", getStackTraceAsString(ex));
    }
    return absent();
  }

  protected final @NonNull Optional<HttpServer> getHttpServer(@NonNull @CheckForNull final Injector injector) {
    try {
      final HttpServer httpServer = checkNotNull(injector).getInstance(HttpServer.class);
      return fromNullable(httpServer);
    } catch(final Exception ex) {
      logger.error("nothing provides HttpServer: {}", getStackTraceAsString(ex));
    }
    return absent();
  }

  protected final void start(@CheckForNull final String[] args, final Consumer<Injector> action) {
    final Injector injector = initialize(checkNotNull(args));

    final Optional<LifecycleManager> maybeManager = getLifecycleManager(injector);

    if (!maybeManager.isPresent()) {
      logger.error("LifecycleManager is unavailable");
      return;
    }

    final LifecycleManager manager = maybeManager.get();

    try {
      manager.start();
    } catch(Exception ex) {
      logger.error("failed to start app: {}", getStackTraceAsString(ex));
      return;
    }

    checkNotNull(action).accept(injector);

    manager.close();
  }

  protected final void startHttpServer(@NonNull @CheckForNull final Injector injector) {
    final Optional<HttpServer> maybeHttpServer = getHttpServer(checkNotNull(injector));

    if (!maybeHttpServer.isPresent()) {
      // nothing provides HTTP server
      System.exit(2);
    }

    // this is blocking
    maybeHttpServer.get().start();
  }

  protected final AppModule newAppModule(@NonNull @CheckForNull final String propertiesFile) {
    checkArgument(!isNullOrEmpty(propertiesFile), "missing properties file");
    return new AppModule(propertiesFile, this.module);
  }

  protected final @NonNull Injector createInjector(@NonNull @CheckForNull final String propertiesFile) {
    final AppModule appModule = newAppModule(propertiesFile);
    return LifecycleInjector.builder().withBootstrapModule(appModule).withModules(appModule).build().createInjector();
  }

  protected final @NonNull Optional<CommandLine> command(@NonNull @CheckForNull final CommandLineParser parser, final String[] args) {
    try {
      return fromNullable(checkNotNull(parser).parse(OPTIONS, args));
    } catch (final ParseException ex) {
      logger.error("failed to parse command line: {}", getStackTraceAsString(ex));
    }
    return absent();
  }

  protected final @NonNull String getOptionOrDefault(@NonNull final CommandLine cmdln,
    @NonNull @CheckForNull final String optionName, @NonNull @CheckForNull final String defaultValue) {
    checkArgument(!isNullOrEmpty(optionName), "missing option name");
    checkArgument(!isNullOrEmpty(defaultValue), "missing default value");
    return cmdln.getOptionValue(optionName, defaultValue);
  }

  protected final @NonNull String getPropertiesFile(@NonNull final CommandLine cmdln) {
    return getOptionOrDefault(cmdln, PROPERTIES_FILE_OPTION, DEFAULT_PROPERTIES_FILE);
  }

}
