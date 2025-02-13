package dev.chux.gcp.crun.gcloud;

import java.util.List;
import java.util.Map;
import java.io.OutputStream;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.assistedinject.Assisted;

import com.google.common.base.Optional;

import ch.vorburger.exec.ManagedProcessBuilder;
import ch.vorburger.exec.ManagedProcessException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

public class GCloudCommandImpl implements GCloudCommand {

  private static final String GCLOUD_COMMAND = "gcloud";

  private final GCloudCommandConfig gcloudCommandConfig;
  private final Optional<OutputStream> stream;
  private final Provider<String> formatProvider;

  @AssistedInject
  public GCloudCommandImpl(@Named("gcloud://format") Provider<String> formatProvider,
    @Assisted GCloudCommandConfig gcloudCommandConfig) {
    this(formatProvider, gcloudCommandConfig, null);
  }

  @AssistedInject
  public GCloudCommandImpl(@Named("gcloud://format") Provider<String> formatProvider,
    @Assisted GCloudCommandConfig gcloudCommandConfig, @Assisted OutputStream stream) {
    this.formatProvider = formatProvider;
    this.gcloudCommandConfig = gcloudCommandConfig;
    this.stream = Optional.fromNullable(stream);
  }

  @Override
  public ManagedProcessBuilder getBuilder() throws ManagedProcessException {
    final ManagedProcessBuilder builder = newGCloudCommandBuilder();
    this.setNamespace(builder)
      .setGroups(builder)
      .setCommand(builder)
      .setFlags(builder)
      .setArguments(builder)
      .setFormat(builder)
      .addOutput(builder);
    return builder;
  } 

  private final String defaultFormat() {
    return this.formatProvider.get();
  }

  private final ManagedProcessBuilder newGCloudCommandBuilder() throws ManagedProcessException {
    return new ManagedProcessBuilder(GCLOUD_COMMAND);
  }

  private final GCloudCommandImpl setNamespace(final ManagedProcessBuilder builder) {
    final Optional<String> namespace = this.gcloudCommandConfig.optionalNamespace();
    if (namespace.isPresent()) {
      builder.addArgument(namespace.get());
    }
    return this;
  }
  
  private final GCloudCommandImpl setCommand(final ManagedProcessBuilder builder) {
    final Optional<String> namespace = this.gcloudCommandConfig.optionalNamespace();
    final String command = this.gcloudCommandConfig.command();
    checkArgument(namespace.isPresent() && !isNullOrEmpty(command), "command is required");
    builder.addArgument(command);
    return this;
  }

  private final GCloudCommandImpl addOutput(final ManagedProcessBuilder builder) {
    if (this.stream.isPresent()) {
      builder.addStdOut(this.stream.get());
    }
    return this;
  }

  private final GCloudCommandImpl setFormat(final ManagedProcessBuilder builder) {
    final Optional<String> format = this.gcloudCommandConfig.optionalFormat();
    builder.addArgument("--format=", format.or(this.defaultFormat()));
    return this;
  }

  private final GCloudCommandImpl addArguments(
    final ManagedProcessBuilder builder, final List<String> arguments) {
    for (final String argument : arguments) {
      builder.addArgument(argument);
    }
    return this;
  }

  private final GCloudCommandImpl setArguments(final ManagedProcessBuilder builder) {
    return this.addArguments(builder, this.gcloudCommandConfig.arguments());
  }

  private final GCloudCommandImpl setGroups(final ManagedProcessBuilder builder) {
    return this.addArguments(builder, this.gcloudCommandConfig.groups());
  }

  private final GCloudCommandImpl setFlags(final ManagedProcessBuilder builder) {
    final Map<String, String> flags = this.gcloudCommandConfig.flags();
    
    for (final Map.Entry<String, String> flag : flags.entrySet()) {
      final String fl = flag.getKey();
      final String value = flag.getValue();

      if (isNullOrEmpty(value)) {
        builder.addArgument("--" + fl);
      } else {
        builder.addArgument("--" + fl + "=", value);
      }
    }
    return this;
  }

}
