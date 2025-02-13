package dev.chux.gcp.crun.faults.command;

import com.google.common.base.Supplier;

import ch.vorburger.exec.ManagedProcess;

import dev.chux.gcp.crun.process.ManagedProcessProvider;
import dev.chux.gcp.crun.process.ManagedMultiProcessProvider;

import static com.google.common.base.Throwables.getStackTraceAsString;

public interface FaultCommand<T>
  extends ManagedProcessProvider, ManagedMultiProcessProvider, Supplier<T> {

  default public int run(final T input) {
    try {
      final ManagedProcess p = this.getBuilder()
        .setDestroyOnShutdown(true)
        .setConsoleBufferMaxLines(0)
        .build().start();
      return p.waitForExit();
    } catch(final Exception ex) {
      System.err.println(getStackTraceAsString(ex));
    }
    return -1;
  }

}
