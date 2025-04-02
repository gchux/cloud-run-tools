package dev.chux.gcp.crun.jmeter;

import java.util.concurrent.Callable;

import com.google.common.base.Supplier;

import com.google.common.util.concurrent.FutureCallback;

public interface JMeterTestExecutor
  extends 
    Callable<JMeterTest>,
    FutureCallback<JMeterTest>,
    Supplier<JMeterTest> {}
