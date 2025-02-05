package dev.chux.gcp.crun.process;

import java.io.OutputStream;

public interface ProcessOutputFactory {

  public ProcessOutput create(OutputStream stream, boolean closeable);

}
