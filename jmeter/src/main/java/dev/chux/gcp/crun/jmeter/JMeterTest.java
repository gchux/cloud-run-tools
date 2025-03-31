package dev.chux.gcp.crun.jmeter;

import java.io.OutputStream;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;

import dev.chux.gcp.crun.process.ProcessProvider;

public interface JMeterTest extends ProcessProvider, Supplier<JMeterTestConfig> {

  public String instanceID();

  public String id();

  public String name();

  public String script();

  public Optional<
    OutputStream
  > stream();

  public long startedAt();

  public long finishedAt();

}
