package dev.chux.gcp.crun.jmeter;

import java.util.Map;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getStackTraceAsString;

public class RequestFileGenerator implements Function<Supplier<JMeterTestConfig>, Optional<Path>> {

  private static final Logger logger = LoggerFactory.getLogger(RequestFileGenerator.class);
  
  private final String LINE_BREAK = System.lineSeparator();
  private final int SIZEOF_LINE_BREAK = LINE_BREAK.length();

  private String newID(
    final JMeterTestConfig config
  ) {
    return Hashing.crc32c().newHasher()
      .putString(config.id(), UTF_8)
      .putLong(System.currentTimeMillis())
      .hash().toString();
  }

  private final Path newRequestFilePath(
    final JMeterTestConfig config
  ) {
    return Paths.get("/tmp/" + this.newID(config));
  }

  private final RequestFileGenerator setQueryParams(
    final JMeterTestConfig config,
    final StringBuilder content
  ) {
    final Optional<Map<String, String>> query = config.query();
    if ( !query.isPresent() ) {
      return this;
    }

    content.append('?');
    for ( final Map.Entry<String, String> param : query.get().entrySet() ) {
      content
        .append(param.getKey())
        .append('=')
        .append(param.getValue())
        .append('&');
    }
    content.append("_");
    return this;
  }

  private final RequestFileGenerator setLine(
    final JMeterTestConfig config,
    final StringBuilder content
  ) {
    content
      .append(
        config.method().or("get").toUpperCase()
      )
      .append(' ')
      .append(config.path().or("/"));
    this.setQueryParams(config, content);
    content
      .append(" HTTP/1.1")
      .append(LINE_BREAK);
    return this;
  }

  private final RequestFileGenerator setHost(
    final JMeterTestConfig config,
    final StringBuilder content
  ) {
    content
      .append("Host: ")
      .append(config.host())
      .append(LINE_BREAK);
    return this;
  }

  private final RequestFileGenerator setHeaders(
    final JMeterTestConfig config,
    final StringBuilder content
  ) {
    final Optional<Map<String, String>> headers = config.headers();
    if ( !headers.isPresent() ) {
      return this;
    }

    for ( final Map.Entry<String, String> header : headers.get().entrySet() ) {
      content
        .append(header.getKey())
        .append(": ")
        .append(header.getValue())
        .append(LINE_BREAK);
    }
    return this;
  }

  private final RequestFileGenerator setTestID(
    final JMeterTestConfig config,
    final StringBuilder content
  ) {
    content
      .append("x-jmaas-test-id: ")
      .append(config.id())
      .append(LINE_BREAK);
    return this;
  }

  private final RequestFileGenerator setPayload(
    final JMeterTestConfig config,
    final StringBuilder content
  ) {
    content
      .append(LINE_BREAK)
      .append(config.body().or(""))
      .append(LINE_BREAK);
    return this;
  }

  private final Optional<Path> writeRequestFile(
    final JMeterTestConfig config,
    final StringBuilder content
  ) {
    logger.info("\n{}", content);
    try {
      return fromNullable(Files.writeString(this.newRequestFilePath(config),
        content.insert(0, content.length()-SIZEOF_LINE_BREAK), UTF_8));
    } catch(final Exception e) {
      logger.error("failed to write request file: {}", getStackTraceAsString(e));
    }
    return Optional.absent();
  }

  public Optional<Path> apply(
    final Supplier<JMeterTestConfig> supplier
  ) {
    final JMeterTestConfig config = checkNotNull(supplier.get());
    final StringBuilder content = new StringBuilder(LINE_BREAK);
    return this
      .setLine(config, content)
      .setHost(config, content)
      .setHeaders(config, content)
      .setTestID(config, content)
      .setPayload(config, content)
      .writeRequestFile(config, content);
  }

}

