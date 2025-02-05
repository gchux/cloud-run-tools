package dev.chux.gcp.crun.process;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.function.Consumer;

class ProcessExecutor implements Consumer<ProcessProvider> {

  public void accept(final ProcessProvider provider) {
    System.err.println(provider);
    try {
      final Process p = provider.getBuilder().start();
      provider.getOutput().from(p.getInputStream());
    } catch(Exception ex) {
      ex.printStackTrace(System.err);
    }
  }

}
