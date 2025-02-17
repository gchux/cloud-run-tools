package dev.chux.gcp.crun.process;

import java.lang.invoke.MethodHandles;

import java.util.Collection;
import java.util.function.Consumer;

import com.google.inject.Inject;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

import dev.chux.gcp.crun.process.ProcessModule.ProcessMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ManagedMultiProcessExecutor implements Consumer<ManagedMultiProcessProvider> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Function<ManagedProcessProvider, ManagedProcessExecution> processMapper;

  @Inject
  ManagedMultiProcessExecutor(
    @ProcessMapper final Function<
      ManagedProcessProvider,
      ManagedProcessExecution
    > processMapper
  ) {
    this.processMapper = processMapper;
  }

  @Override
  public void accept(final ManagedMultiProcessProvider provider) {
    this.execute(provider.getProviders());
  }

  private final void execute(final Collection<ManagedProcessProvider> providers) {
    Flowable
      .fromIterable(providers)
      .parallel()
      .runOn(Schedulers.io())
      .map(this.processMapper)
      .sequential()
      .observeOn(Schedulers.computation())
      .blockingForEach((final ManagedProcessExecution execution) -> {
        logger.info("{} => {}", execution.provider(), execution.exitCode());
      });
  }

}
