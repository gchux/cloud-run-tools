package dev.chux.gcp.crun.jmeter;

import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.assistedinject.Assisted;
import dev.chux.gcp.crun.process.ProcessProvider;
import dev.chux.gcp.crun.process.ProcessOutput;
import dev.chux.gcp.crun.process.ProcessOutputFactory;

public interface JMeterTest extends ProcessProvider {

}
