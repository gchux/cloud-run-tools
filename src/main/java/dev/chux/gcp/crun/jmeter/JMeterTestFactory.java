package dev.chux.gcp.crun.jmeter;

import java.io.OutputStream;

public interface JMeterTestFactory {

  public JMeterTest create(JMeterTestConfig jMeterTestConfig);

  public JMeterTest createWithNonCloseableOutputStream(JMeterTestConfig jMeterTestConfig, OutputStream stream);

  public JMeterTest createWithOutputStream(JMeterTestConfig jMeterTestConfig, OutputStream stream, boolean closeable);

}
