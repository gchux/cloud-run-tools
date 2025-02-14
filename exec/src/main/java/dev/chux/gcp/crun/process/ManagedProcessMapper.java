package dev.chux.gcp.crun.process;

import java.lang.invoke.MethodHandles;

import com.google.inject.Inject;

import com.google.common.base.Function;

import dev.chux.gcp.crun.process.ProcessModule.ProcessConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ManagedProcessMapper implements
  io.reactivex.rxjava3.functions.Function<ManagedProcessProvider, ManagedProcessExecution> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Function<ManagedProcessProvider, Integer> processConsumer;

  @Inject
  ManagedProcessMapper(
    @ProcessConsumer final Function<
      ManagedProcessProvider, Integer
    > processConsumer
  ) {
    this.processConsumer = processConsumer;
  }

  @Override
  public ManagedProcessExecution apply(final ManagedProcessProvider provider) {
    final Integer exitCode = this.processConsumer.apply(provider);
    logger.info("{} => {}", provider, exitCode);
    return new ManagedProcessExecution(provider, exitCode);
  }

}

