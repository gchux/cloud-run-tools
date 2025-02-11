package dev.chux.gcp.crun;

import java.util.function.Consumer;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import com.google.common.base.Optional;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import dev.chux.gcp.crun.http.HttpModule;
import dev.chux.gcp.crun.rest.RestModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getStackTraceAsString;

public class RestApp extends AbstractApp {
  private static final Logger logger = LoggerFactory.getLogger(RestApp.class);

  private RestApp(@CheckForNull final Module coreModule, final Optional<Module> appModule) {
    super(checkNotNull(coreModule), appModule); 
  }

  public static RestApp of() {
    return of(null);
  }

  public static RestApp of(@Nullable final Module module) {
    final Module coreModule = Modules.combine(new RestModule(), new HttpModule());
    return new RestApp(coreModule, fromNullable(module));
  }

  public void run(final String[] args) {
    this.run((ignored) -> {}, args);
  }

  @Override
  public void run(final Consumer<Injector> action, final String[] args) {
    final RestApp app = this;
    this.start(args, (final Injector injector) -> {
      try {
        action.accept(injector);
        app.startHttpServer(injector);
      } catch(final Exception ex) {
        logger.error("failed to start HttpServer: {}", getStackTraceAsString(ex));
      }
    });
  }

}
