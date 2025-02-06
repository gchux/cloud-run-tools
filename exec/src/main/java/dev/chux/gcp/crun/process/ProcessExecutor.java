package dev.chux.gcp.crun.process;

import java.lang.invoke.MethodHandles;

import java.io.InputStreamReader;
import java.io.BufferedReader;

import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ProcessExecutor implements Consumer<ProcessProvider> {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final boolean IS_OS_WINDOWS = System.getProperty("os.name").startsWith("Windows");

  public void accept(final ProcessProvider provider) {
    logger.info("provider: {}", provider);

    final ProcessBuilder builder = provider.getBuilder();
    builder.command(fixArguments(builder));
    
    try {
      final Process p = builder.start();
      provider.getOutput().from(p.getInputStream());
    } catch(Exception ex) {
      logger.error("process execution failed", ex);
    }
  }

  // see: https://github.com/zeroturnaround/zt-exec/blob/zt-exec-1.12/src/main/java/org/zeroturnaround/exec/ProcessExecutor.java#L1226-L1245
  private static List<String> fixArguments(final ProcessBuilder builder) {
    final List<String> command = builder.command();

    if (!IS_OS_WINDOWS) {
      return ImmutableList.copyOf(command);
    }

    final List<String> result = Lists.newArrayList(command);
    for (final ListIterator it = result.listIterator(); it.hasNext(); ) {
      if ("".equals(it.next())) {
        it.set("\"\"");
      }
    }
    return ImmutableList.copyOf(result);
  }

}
