package dev.chux.gcp.crun.jmeter;

import java.io.OutputStream;

import com.google.common.base.Supplier;

import dev.chux.gcp.crun.process.ProcessProvider;

public interface JMeterTest extends ProcessProvider, Supplier<JMeterTestConfig> {

  public String id();

  public String name();

  public OutputStream stream();

}
