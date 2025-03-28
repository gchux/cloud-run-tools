package dev.chux.gcp.crun.jmeter;

import java.util.concurrent.ConcurrentMap;

import com.google.inject.Inject;

import com.google.common.collect.ForwardingConcurrentMap;

public class JMeterTestStorage extends ForwardingConcurrentMap<String, JMeterTest> {
  
  private final ConcurrentMap<String, JMeterTest> delegate;

  @Inject
  public JMeterTestStorage(
    final ConcurrentMap<
      String, JMeterTest
    > delegate
  ) {
    this.delegate = delegate;
  }

  protected ConcurrentMap<
    String, JMeterTest
  > delegate() {
    return this.delegate();
  }

}
