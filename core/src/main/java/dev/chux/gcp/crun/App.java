package dev.chux.gcp.crun;

import java.util.function.Consumer;

import com.google.inject.Injector;
import com.google.inject.Module;

import com.google.common.base.Optional;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getStackTraceAsString;

public class App extends AbstractApp {
  private static final Logger logger = LoggerFactory.getLogger(App.class);

  private App(final Optional<Module> appModule) {
    super(appModule); 
  }

  public static App of() {
    return new App(absent());
  }

  public static App of(@Nullable final Module module) {
    return new App(fromNullable(module));
  }

  private final void waitAndExit(final AppMainThread mainThread) {
    logger.info("waiting for app to exit");
    
    int exitCode = 0;
    try {
      exitCode = checkNotNull(mainThread).await();
    } catch(final Exception ex) {
      exitCode = 1;
      logger.error("failed to start app: {}", getStackTraceAsString(ex));
    }

    logger.info("app exited with code: {}", exitCode);
    
    System.exit(exitCode);
  }

  private final void addShutdownHook(final Runnable mainThread) {
    final Thread hook = new Thread(checkNotNull(mainThread));
    Runtime.getRuntime().addShutdownHook(hook);
    logger.info("registered JVM shutdown hook");
  }

  private final void handleAppMainThread(final Optional<AppMainThread> appMainThread) {
    if (!appMainThread.isPresent()) {
      logger.warn("app does not provide a main thread");
      return;
    }

    final AppMainThread mainThread = appMainThread.get();

    this.addShutdownHook(mainThread);
    this.waitAndExit(mainThread);
  }

  private final void startApp(final Injector injector) {
    final Optional<AppMainThread> appMainThread = getAppMainThread(injector);
    this.handleAppMainThread(appMainThread);
  }

  @Override
  public void run(final Consumer<Injector> action, final String[] args) {
    final App app = this;
    this.start(args, (final Injector injector) -> {
      action.accept(injector);
      this.startApp(injector);
    });
  }

  public void run(final String[] args) {
    this.run((ignored) -> {}, args);
  }

}
