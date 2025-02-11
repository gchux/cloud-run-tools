package dev.chux.gcp.crun.curl;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;

import com.google.common.base.Optional;

import org.apache.commons.cli.*;

import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static java.net.http.HttpRequest.BodyPublishers.ofString;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

public final class App {

  private static final String OPTION_REQUEST = "request";
  private static final String OPTION_HEADER = "header";
  private static final String OPTION_DATA_RAW = "data-raw";

  private static final Options OPTIONS = new Options();

  static {
    OPTIONS.addOption("X", OPTION_REQUEST, true, "HTTP method to be used");

    final Option option = new Option("H", "header", true, "HTTP request metadata; headets to be sent");
    option.setArgs(Option.UNLIMITED_VALUES);
    OPTIONS.addOption(option);
    
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
    final BodyPublisher body = body(cmdln);

    final HttpClient client = newHttpClient();
    final HttpRequest request = newHttpRequest(url, method, body);

    exec(client, request);
  }

  private static final void exec(final HttpClient client, final HttpRequest request) {
    try {
      final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      System.out.println(response.body());  
    } catch(Exception ex) {
      ex.printStackTrace(System.err);
    }
  }

  private static final HttpClient newHttpClient() {
    return HttpClient.newBuilder()
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build();
  }

  private static final HttpRequest newHttpRequest(final String url, final String method, final BodyPublisher body) {
    return HttpRequest.newBuilder()
      .uri(URI.create(url))
      .timeout(Duration.ofMinutes(2))
      .method(method, body).build();
  }

  private static final BodyPublisher body(final CommandLine cmdln) {
    final String data = data(cmdln);
    return isNullOrEmpty(data)? noBody() : ofString(data);
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

  private static final String[] headers(final CommandLine cmdln) {
    return options(cmdln, OPTION_HEADER);
  }

  private static final String URL(final CommandLine cmdln) {
    final String[] args = cmdln.getArgs();
    if (args.length == 0) {
      return "";
    }
    return args[0];
  }

}
