package dev.chux.gcp.crun.curl;

import java.util.List;
import java.util.Map;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.URI;

import java.time.Duration;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;

import org.apache.commons.cli.*;

import static java.net.http.HttpRequest.BodyPublishers.noBody;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

public final class App {

  private static final String OPTION_REQUEST = "request";
  private static final String OPTION_HEADER = "header";
  private static final String OPTION_DATA_RAW = "data-raw";

  // must match: https://curl.se/docs/manpage.html
  private static final Options OPTIONS = new Options();

  private static final Splitter headersSplitter = Splitter.on(':').trimResults();

  static {
    // https://curl.se/docs/manpage.html#-X
    OPTIONS.addOption("X", OPTION_REQUEST, true, "HTTP method to be used");

    // https://curl.se/docs/manpage.html#-H
    final Option option = new Option("H", "header", true, "HTTP request metadata; headets to be sent");
    option.setArgs(Option.UNLIMITED_VALUES);
    OPTIONS.addOption(option);
    
    // https://curl.se/docs/manpage.html#--data-raw
    OPTIONS.addOption(OPTION_DATA_RAW, true, "data to be sent in the HTTP request payload");
  }

  public static void main(final String[] args) {
    final CommandLineParser parser = new DefaultParser();
    final Optional<CommandLine> maybeCmdln = command(parser, args);

    if (!maybeCmdln.isPresent()) {
      // error parsing command line
      System.err.println("invlid command line");
      System.exit(1);
    }

    final CommandLine cmdln = maybeCmdln.get();

    final String url = URL(cmdln);
    
    if (isNullOrEmpty(url)) {
      System.err.println("missing URL");
      System.exit(1);
    }

    final String method = method(cmdln);
    final String data = data(cmdln);
    final BodyPublisher body = body(data);
    final Map<String, String> headers = headers(cmdln);

    final HttpClient client = newHttpClient();
    final HttpRequest request = newHttpRequest(url, method, headers, body);

    logRequest(request, data);

    exec(client, request);
  }

  private static final void logHeader(final String name, final List<String> value) {
      System.out.print("\t- Header[");
      System.out.print(name);
      System.out.print("]=");
      System.out.println(value);
  }

  private static final void logHeaders(final Map<String, List<String>> headers) {
    for(final Map.Entry<String, List<String>> header : headers.entrySet()) {
      logHeader(header.getKey(), header.getValue());
    }
  }

  private static final void logRequest(final HttpRequest request, final String body) {
    System.out.print("\n* Request: ");
    System.out.println(request.method());  
    System.out.println("\n* Request Headers:");
    logHeaders(request.headers().map());
    System.out.print("\n* Request Body:\n\t");
    System.out.println(body);
  }

  private static final void logResponse(final HttpResponse<String> response) {
    System.out.print("\n* Response: ");
    System.out.println(response.statusCode());  
    System.out.println("\n* Response Headers:");
    logHeaders(response.headers().map());
    System.out.print("\n* Response Body:\n\t");
    System.out.println(response.body());
  }

  private static final void exec(final HttpClient client, final HttpRequest request) {
    try {
      final BodyHandler<String> handler = BodyHandlers.ofString();
      final HttpResponse<String> response = client.send(request, handler);
      logResponse(response);
    } catch(Exception ex) {
      ex.printStackTrace(System.err);
    }
  }

  private static final HttpClient newHttpClient() {
    return HttpClient.newBuilder()
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build();
  }

  private static final HttpRequest newHttpRequest(
    final String url,
    final String method,
    final Map<String, String> headers,
    final BodyPublisher body
  ) {
    final HttpRequest.Builder builder = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .timeout(Duration.ofMinutes(2))
      .method(method, body);
    return setHeaders(builder, headers).build();
  }

  private static final BodyPublisher body(final String data) {
    return isNullOrEmpty(data)? noBody() : BodyPublishers.ofString(data);
  }

  private static final BodyPublisher body(final CommandLine cmdln) {
    return body(data(cmdln));
  }

  private static final Optional<CommandLine> command(final CommandLineParser parser, final String[] args) {
    try {
      return fromNullable(checkNotNull(parser).parse(OPTIONS, args));
    } catch (final ParseException ex) {
      ex.printStackTrace(System.err);
    }
    return absent();
  }

  private static final String option(final CommandLine cmdln, final String optionName) {
    checkArgument(!isNullOrEmpty(optionName), "missing option name");
    return cmdln.getOptionValue(optionName);
  }

  private static final String[] options(final CommandLine cmdln, final String optionName) {
    checkArgument(!isNullOrEmpty(optionName), "missing option name");
    return cmdln.getOptionValues(optionName);
  }

  private static final String method(final CommandLine cmdln) {
    String method = option(cmdln, OPTION_REQUEST);
    if (isNullOrEmpty(method)) {
      method = "GET";
    }
    return method;
  }

  private static final String data(final CommandLine cmdln) {
    return option(cmdln, OPTION_DATA_RAW);
  }

  private static final HttpRequest.Builder setHeaders(final HttpRequest.Builder builder, final Map<String, String> headers) {
    for(final Map.Entry<String, String> header : headers.entrySet()) {
      builder.header(header.getKey(), header.getValue());
    }
    return builder;
  }

  private static final void setHeader(final ImmutableMap.Builder<String, String> headers, final String rawHeader) {
    final List<String> parts = headersSplitter.splitToList(rawHeader);
    if (parts.size() != 2) {
      return;
    }
    headers.put(parts.get(0), parts.get(1));
  }

  private static final Map<String, String> headers(final CommandLine cmdln) {
    final String[] headers = options(cmdln, OPTION_HEADER);
    if (headers == null) {
      return ImmutableMap.of();
    }

    final ImmutableMap.Builder<String, String> headersMap = ImmutableMap.<String, String>builder();
    for(final String header : headers) {
      setHeader(headersMap, header);
    }
    return headersMap.build();
  }

  private static final String URL(final CommandLine cmdln) {
    final String[] args = cmdln.getArgs();
    if (args.length == 0) {
      return "";
    }
    return args[0];
  }

}
