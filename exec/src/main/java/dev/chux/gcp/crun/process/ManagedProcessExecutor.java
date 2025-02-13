package dev.chux.gcp.crun.process;

import java.lang.invoke.MethodHandles;

import java.util.function.Consumer;

import com.google.common.base.Function;

import ch.vorburger.exec.ManagedProcess;
import ch.vorburger.exec.ManagedProcessBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Throwables.getStackTraceAsString;

class ManagedProcessExecutor implements Consumer<ManagedProcessProvider>, Function<ManagedProcessProvider, Integer> {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public void accept(final ManagedProcessProvider provider) {
    logger.info("provider: {}", provider);
    this.apply(provider);
  }

  public Integer apply(final ManagedProcessProvider provider) {
    try {
      final ManagedProcessBuilder builder = provider.getBuilder()
        .setDestroyOnShutdown(true).setConsoleBufferMaxLines(0);
      return this.execute(builder.build());
    } catch(Exception ex) {
      logger.error("process failed: {}", getStackTraceAsString(ex));
    }
    return Integer.valueOf(-1);
  }

  private final Integer execute(final ManagedProcess process) throws Exception {
    process.start();
    final Integer exitCode = Integer.valueOf(process.waitForExit());
    logger.debug("{} => {}", process.getProcLongName(), exitCode);
    return exitCode;
  }

}
