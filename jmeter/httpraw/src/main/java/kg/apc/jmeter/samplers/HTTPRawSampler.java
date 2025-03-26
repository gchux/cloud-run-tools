package kg.apc.jmeter.samplers;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.StandardSocketOptions;

import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.charset.StandardCharsets;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SNIHostName;

import kg.apc.io.SocketChannelWithTimeouts;

import com.google.common.io.ByteStreams;
import com.google.common.primitives.UnsignedLong;

import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.util.JMeterUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tlschannel.ClientTlsChannel;
import tlschannel.SniSslContextFactory;
import tlschannel.TlsChannel;

public class HTTPRawSampler extends AbstractIPSampler {
    
    private static final String FILE_NAME = "fileName";
    private static final String KEEPALIVE = "keepalive";
    private static final String HTTPS = "https";
    private static final String PARSE = "parse";

    private static final String RNpattern = "\\r\\n";
    private static final String SPACE = " ";
    // 
    private static final Logger log = LoggerFactory.getLogger(HTTPRawSampler.class);
    private static final Pattern anyContent = Pattern.compile(".+", Pattern.DOTALL);
    private SocketChannel savedSock;
    private static final int fileSendingChunk = JMeterUtils.getPropDefault("kg.apc.jmeter.samplers.FileReadChunkSize", 1024 * 4);
    
    public HTTPRawSampler() {
        super();
        log.debug("File reading chunk size: " + fileSendingChunk);
    }
    
    @Override
    public SampleResult sample(Entry entry) {
        SampleResult res = super.sample(entry);
        if (isParseResult()) {
            parseResponse(res);
        }
        return res;
    }
    
    protected byte[] readResponse(
        final ByteChannel channel,
        SampleResult res
    ) throws IOException {
        final ByteArrayOutputStream response = new ByteArrayOutputStream();
        
        long responseSize = 0;
        
        if (log.isDebugEnabled()) {
            log.debug("Start reading response");
        }

        try {
            responseSize = ByteStreams.copy(channel, Channels.newChannel(response));
            if (response.size() < 1) {
                log.warn("Read no bytes from socket, seems it was closed. Let it be so.");
                channel.close();
            }
        } catch (IOException ex) {
            channel.close();
            throw ex;
        }
        
        if (log.isDebugEnabled()) {
            log.debug("Done reading response");
        }
        
        res.sampleEnd();
        if (!isUseKeepAlive()) {
            channel.close();
        }
        
        res.setBytes(
            UnsignedLong.valueOf(responseSize).intValue()
        );
        return response.toByteArray();
    }
    
    private void parseResponse(SampleResult res) {
        Scanner scanner = new Scanner(res.getResponseDataAsString());
        scanner.useDelimiter(RNpattern);
        
        if (!scanner.hasNextLine()) {
            return;
        }
        
        String httpStatus = scanner.nextLine();
        
        int s = httpStatus.indexOf(SPACE);
        int e = httpStatus.indexOf(SPACE, s + 1);
        if (s < e) {
            String rc = httpStatus.substring(s, e).trim();
            try {
                int rcInt = Integer.parseInt(rc);
                if (rcInt < 100 || rcInt > 599) {
                    return;
                }
                res.setResponseCode(rc);
                res.setResponseMessage(httpStatus.substring(e).trim());
            } catch (NumberFormatException ex) {
                return;
            }
        } else {
            return;
        }

        if (!scanner.hasNextLine()) {
            return;
        }

        StringBuilder headers = new StringBuilder();
        String line;
        while (scanner.hasNextLine() && !(line = scanner.nextLine()).isEmpty()) {
            headers.append(line).append(CRLF);
        }
        res.setResponseHeaders(headers.toString());
        
        if (scanner.hasNext()) {
            res.setResponseData(scanner.next(anyContent).getBytes());
        } else {
            res.setResponseData("".getBytes());
        }
        
    }
    
    @Override
    protected byte[] processIO(SampleResult res) throws Exception {
        final ByteChannel socket = getSocketChannel();
        if (!getRequestData().isEmpty()) {
            final ByteBuffer sendBuf = ByteBuffer.wrap(
                getRequestData().getBytes(StandardCharsets.UTF_8)
            );
            socket.write(sendBuf);
        }
        sendFile(getFileToSend(), socket);
        if (log.isDebugEnabled()) {
            log.debug("Sent request");
        }
        return readResponse(socket, res);
    }
    
    protected ByteChannel getSocketChannel() throws Exception {
        int port;
        try {
            port = Integer.parseInt(getPort());
        } catch (NumberFormatException ex) {
            log.warn("Wrong port number: " + getPort() + ", defaulting to 80", ex);
            port = 80;
        }

        final InetSocketAddress address = new InetSocketAddress(getHostName(), port);

        if ( isHTTPS() ) {
            // bypass to support HTTPS
            return getSecureChannel(address);
        }

        final SocketChannel socket = getChannel();
        socket.connect(address);
        return socket;
    }

    private final ByteChannel getSecureChannel(
        final InetSocketAddress address
    ) throws Exception {
        final String serverName = address.getHostName();

        final int t = getTimeoutAsInt();

        final SocketChannel socket = SocketChannel.open();
        socket.setOption(
            StandardSocketOptions.SO_KEEPALIVE, Boolean.FALSE
        );

        final Socket s = socket.socket();
        s.setKeepAlive(false);
        s.setSoTimeout(t);
        s.connect(address, t);

        log.info("{} > {}", s.getLocalSocketAddress(), address);

        final SSLContext sslContext = SSLContext.getDefault();

        final SSLEngine engine = sslContext
        .createSSLEngine(
            serverName,
            address.getPort()
        );
        engine.setUseClientMode(true);

        // ALL Google APIs and Services require SNI
        final SSLParameters sslParams = engine.getSSLParameters();
        sslParams.setEndpointIdentificationAlgorithm("HTTPS");
        sslParams.setServerNames(
            Collections.singletonList(
                new SNIHostName(serverName)
            )
        );

        engine.setSSLParameters(sslParams);

        return ClientTlsChannel.newBuilder(socket, engine).build();
    }
    
    @Override
    protected SocketChannel getChannel() throws IOException {
        int t = getTimeoutAsInt();
        if (t > 0) {
            SocketChannelWithTimeouts res =
                (SocketChannelWithTimeouts) SocketChannelWithTimeouts.open();
            res.setConnectTimeout(t);
            res.setReadTimeout(t);
            return res;
        } else {
            return SocketChannel.open();
        }
    }
    
    public boolean isUseKeepAlive() {
        return getPropertyAsBoolean(KEEPALIVE);
    }
    
    public void setUseKeepAlive(boolean selected) {
        setProperty(KEEPALIVE, selected);
    }
    
    public boolean isHTTPS() {
        return getPropertyAsBoolean(HTTPS);
    }
    
    public void setIsHTTPS(final boolean selected) {
        setProperty(HTTPS, selected);
    }
    
    public boolean isParseResult() {
        return getPropertyAsBoolean(PARSE);
    }
    
    public void setParseResult(boolean selected) {
        setProperty(PARSE, selected);
    }
    
    public String getFileToSend() {
        return getPropertyAsString(FILE_NAME);
    }
    
    public void setFileToSend(String text) {
        setProperty(FILE_NAME, text);
    }
    
    @Override
    public boolean interrupt() {
        if (savedSock != null && savedSock.isOpen()) {
            try {
                savedSock.close();
            } catch (IOException ex) {
                log.warn("Exception while interrupting channel: ", ex);
                return false;
            }
        }
        return true;
    }
    
    private void sendFile(
        final String filename,
        final ByteChannel socket
    ) throws IOException {
        if (filename.isEmpty()) {
            return;
        }
        
        final ReadableByteChannel source = Files.newByteChannel(
            Paths.get(filename), EnumSet.of(StandardOpenOption.READ)
        );

        ByteStreams.copy(source, socket);
        source.close();
    }
}
