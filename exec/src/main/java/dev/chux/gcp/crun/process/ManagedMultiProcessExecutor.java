package dev.chux.gcp.crun.process;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.function.Consumer;

import com.google.inject.Inject;

import dev.chux.gcp.crun.process.ProcessModule.ProcessConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ManagedMultiProcessExecutor implements Consumer<ManagedMultiProcessProvider> {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Consumer<ManagedProcessProvider> processConsumer;

  @Inject
  ManagedMultiProcessExecutor(
    @ProcessConsumer final Consumer<ManagedProcessProvider> processConsumer
  ) {
    this.processConsumer = processConsumer;
  }

  public void accept(final ManagedMultiProcessProvider provider) {
    this.execute(provider.getProviders());
  }

  private final void execute(final Collection<ManagedProcessProvider> providers) {
    for(final ManagedProcessProvider provider : providers) {
      logger.info("provider: {}", provider);
      this.processConsumer.accept(provider);
    }
  }

}
