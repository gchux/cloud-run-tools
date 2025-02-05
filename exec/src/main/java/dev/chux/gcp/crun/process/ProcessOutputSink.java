package dev.chux.gcp.crun.process;

import com.google.common.io.ByteSink;
import com.google.common.io.ByteStreams;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import dev.chux.gcp.crun.process.ProcessOutput;
import dev.chux.gcp.crun.process.ProcessOutputSink;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class ProcessOutputSink
    implements ProcessOutput {
    private final OutputStream stream;
    private final boolean closeable;
    private final ByteSink sink;

    @AssistedInject
    ProcessOutputSink(@Assisted OutputStream stream, @Assisted boolean closeable) {
        this.stream = stream;
        this.closeable = closeable;
        this.sink = new Sink(stream);
    }

    public long from(InputStream stream) throws IOException {
        if (this.closeable) {
            return this.sink(stream);
        }
        return this.copy(stream);
    }

    private long copy(InputStream stream) throws IOException {
        return ByteStreams.copy((InputStream)stream, (OutputStream)this.stream);
    }

    private long sink(InputStream stream) throws IOException {
        return this.sink.writeFrom(stream);
    }

    public ByteSink get() {
        return this.sink;
    }

    private static class Sink extends ByteSink {

        final OutputStream stream;

        private Sink(OutputStream stream) {
            this.stream = stream;
        }

        public OutputStream openStream() {
            return this.stream;
        }
    }

}
