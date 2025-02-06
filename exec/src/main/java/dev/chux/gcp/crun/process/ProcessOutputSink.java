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

class ProcessOutputSink implements ProcessOutput {

    private final OutputStream stream;
    private final boolean closeable;
    private final ByteSink sink;

    @AssistedInject
    ProcessOutputSink(@Assisted OutputStream stream, @Assisted boolean closeable) {
        this.stream = stream;
        this.closeable = closeable;
        this.sink = new Sink(stream);
    }

    public long from(final InputStream stream) throws IOException {
        if (this.closeable) {
            return this.sink(stream);
        }
        return this.copy(stream);
    }

    private long copy(final InputStream stream) throws IOException {
        // `copy(...)` will not close streams;
        // see: https://github.com/google/guava/blob/v33.4.0/guava/src/com/google/common/io/ByteStreams.java#L99-L112
        return ByteStreams.copy((InputStream) stream, (OutputStream) this.stream);
    }

    private long sink(final InputStream stream) throws IOException {
        // `writeFrom(...)` closes the stream;
        // see: https://github.com/google/guava/blob/v33.4.0/guava/src/com/google/common/io/ByteSink.java#L118-L120
        return this.sink.writeFrom(stream);
    }

    public ByteSink get() {
        return this.sink;
    }

    private static class Sink extends ByteSink {

        private final OutputStream stream;

        private Sink(OutputStream stream) {
            this.stream = stream;
        }

        public OutputStream openStream() {
            return this.stream;
        }
    }

}
