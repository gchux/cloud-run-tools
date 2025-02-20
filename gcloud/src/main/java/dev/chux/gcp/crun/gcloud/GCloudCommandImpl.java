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

  private final dev.chux.gcp.crun.model.GCloudCommand gcloudCommand;
  private final Optional<OutputStream> stream;
  private final Provider<String> formatProvider;

  @AssistedInject
  public GCloudCommandImpl(
    @Named(GCloudFormatSupplier.KEY) Provider<String> formatProvider,
    @Assisted dev.chux.gcp.crun.model.GCloudCommand gcloudCommand
  ) {
    this(formatProvider, gcloudCommand, null);
  }

  @AssistedInject
  public GCloudCommandImpl(
    @Named(GCloudFormatSupplier.KEY) Provider<String> formatProvider,
    @Assisted dev.chux.gcp.crun.model.GCloudCommand gcloudCommand,
    @Assisted OutputStream stream
  ) {
    this.formatProvider = formatProvider;
    this.gcloudCommand = gcloudCommand;
    this.stream = Optional.fromNullable(stream);
  }

  @Override
  public ManagedProcessBuilder getBuilder() throws ManagedProcessException {
    final ManagedProcessBuilder builder = newGCloudCommandBuilder();

    this.setEnvironment(builder)
      .setNamespace(builder)
      .setGroups(builder)
      .setCommand(builder)
      .setFlags(builder)
      .setProject(builder)
      .setFormat(builder)
      .setArguments(
        builder.addArgument("--quiet")
      ).addOutput(builder);

    return builder;
  } 

  private final String defaultFormat() {
    return this.formatProvider.get();
  }

  private final ManagedProcessBuilder newGCloudCommandBuilder() throws ManagedProcessException {
    return new ManagedProcessBuilder(GCLOUD_COMMAND);
  }

  private final void setEnvVar(
    final Map<String, String> env,
    final String name, final String value
  ) {
    if (!isNullOrEmpty(name) && !isNullOrEmpty(value)) {
      env.put(name, value);
    }
  }

  private final void setEnvVar(
    final Map<String, String> env,
    final Map.Entry<String, String> variable
  ) {
    this.setEnvVar(env, variable.getKey(), variable.getValue());
  }

  private final GCloudCommandImpl setEnvironment(
    final ManagedProcessBuilder builder
  ) {
    final Map<String, String> env = builder.getEnvironment();

    this.setEnvVar(env, "CLOUDSDK_CORE_DISABLE_PROMPTS", "1");

    final Map<String, String> environment = this.gcloudCommand.environment();
    for (final Map.Entry<String, String> variable : environment.entrySet()) {
      this.setEnvVar(env, variable);
    }

    return this;
  }

  private final GCloudCommandImpl setFlag(
    final ManagedProcessBuilder builder,
    final String name, final String value
  ) {
    if (isNullOrEmpty(value)) {
      builder.addArgument("--" + name);
    } else {
      builder.addArgument("--" + name + "=" + value);
    }
    return this;
  }

  private final GCloudCommandImpl setNamespace(final ManagedProcessBuilder builder) {
    final Optional<String> namespace = this.gcloudCommand.optionalNamespace();
    if (namespace.isPresent()) {
      builder.addArgument(namespace.get());
    }
    return this;
  }
  
  private final GCloudCommandImpl setCommand(final ManagedProcessBuilder builder) {
    final String command = this.gcloudCommand.command();
    if (!isNullOrEmpty(command)) {
      builder.addArgument(command);
    }
    return this;
  }

  private final GCloudCommandImpl addOutput(final ManagedProcessBuilder builder) {
    if (this.stream.isPresent()) {
      builder.addStdOut(this.stream.get());
    }
    return this;
  }

  private final GCloudCommandImpl setProject(final ManagedProcessBuilder builder) {
    final Optional<String> project = this.gcloudCommand.optionalProject();
    if (project.isPresent()) {
      return this.setFlag(builder, "project", project.get());
    }
    return this;
  }

  private final GCloudCommandImpl setFormat(final ManagedProcessBuilder builder) {
    final Optional<String> format = this.gcloudCommand.optionalFormat();
    return this.setFlag(builder, "format", format.or(this.defaultFormat()));
  }

  private final GCloudCommandImpl addArguments(
    final ManagedProcessBuilder builder, final List<String> arguments) {
    for (final String argument : arguments) {
      builder.addArgument(argument);
    }
    return this;
  }

  private final GCloudCommandImpl setArguments(final ManagedProcessBuilder builder) {
    return this.addArguments(builder, this.gcloudCommand.arguments());
  }

  private final GCloudCommandImpl setGroups(final ManagedProcessBuilder builder) {
    return this.addArguments(builder, this.gcloudCommand.groups());
  }

  private final GCloudCommandImpl setFlags(final ManagedProcessBuilder builder) {
    final Map<String, String> flags = this.gcloudCommand.flags();
    for (final Map.Entry<String, String> flag : flags.entrySet()) {
      this.setFlag(builder, flag.getKey(), flag.getValue());
    }
    return this;
  }

}
