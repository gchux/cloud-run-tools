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

  @Override
  public void run(final Consumer<Injector> action, final String[] args) {
    try {
      this.start(args, action);
    } catch(final Exception ex) {
      logger.error("failed to start app: {}", getStackTraceAsString(ex));
    }
  }

}
