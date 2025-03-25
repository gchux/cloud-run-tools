package dev.chux.gcp.crun.jmeter;

import java.util.Map;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;

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

  private final Path newRequestFilePath(
    final JMeterTestConfig config
  ) {
    return Paths.get("/tmp/" + config.name());
  }

  private final Path newRequestBodyFilePath(final Path requestFilePath) {
    return requestFilePath.resolveSibling(
      requestFilePath.getFileName() + "_body"
    );
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

  private final RequestFileGenerator setPayload(
    final JMeterTestConfig config,
    final StringBuilder content
  ) {
    final String body = config.body().or("");
    content
      .append("Content-Length: ")
      .append(body.length())
      .append(LINE_BREAK)
      .append(LINE_BREAK)
      .append(body);
    return this;
  }

  private final Optional<Path> writeRequestFile(
    final JMeterTestConfig config,
    final StringBuilder content
  ) {
    final String body = config.body().or("");

    // set `Content-Length`
    content
      .append("Content-Length: ")
      .append(body.length());

    logger.info("\n{}\n\n{}", content, body);
    try {
      final Optional<Path> path = fromNullable(
        Files.writeString(
          this.newRequestFilePath(config),
          content.insert(0, content.length()-SIZEOF_LINE_BREAK),
          UTF_8
        )
      );
      if ( path.isPresent() ) {
        final Path p = path.get();
        // write request body to its own file
        Files.writeString(
          this.newRequestBodyFilePath(p), body, UTF_8
        );
        return path;
      }
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
      .setHeaders(config, content)
      .writeRequestFile(config, content);
  }

}

