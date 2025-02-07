package dev.chux.gcp.crun.process;

import java.lang.invoke.MethodHandles;

import java.util.function.Consumer;

import ch.vorburger.exec.ManagedProcess;
import ch.vorburger.exec.ManagedProcessBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ManagedProcessExecutor implements Consumer<ManagedProcessProvider> {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public void accept(final ManagedProcessProvider provider) {
    logger.info("provider: {}", provider);

    try {
      final ManagedProcessBuilder builder = provider.getBuilder()
        .setDestroyOnShutdown(true).setConsoleBufferMaxLines(0);
      final ManagedProcess p = builder.build();
      p.start();
      p.waitForExit();
    } catch(Exception ex) {
      logger.error("process execution failed", ex);
    }
  }

}
