package dev.chux.gcp.crun.process;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;
import com.google.common.io.ByteSink;

public interface ProcessOutput extends Supplier<ByteSink> {

  public long from(final InputStream stream) throws IOException;

}
