package dev.chux.gcp.crun.gcloud;

import java.util.List;
import java.util.Map;
import java.util.Set;

import java.io.OutputStream;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.assistedinject.Assisted;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import ch.vorburger.exec.ManagedProcessBuilder;
import ch.vorburger.exec.ManagedProcessException;

import dev.chux.gcp.crun.ConfigService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

public class GCloudCommandImpl implements GCloudCommand {

  private static final Logger logger = LoggerFactory.getLogger(GCloudCommandImpl.class);

  private static final String DEFAULT_GCLOUD_COMMAND = "gcloud";

  private static final String PROPERTY_PREFIX = "gcloud.";
  private static final String PROPERTY_GCLOUD_COMMAND = PROPERTY_PREFIX + "command";
  private static final String PROPERTY_GCLOUD_ENVIRONMENT_BLACKLIST = PROPERTY_PREFIX + "environment.blacklist";

  private final String gcloudBinary;
  private final dev.chux.gcp.crun.model.GCloudCommand gcloudCommand;
  private final Optional<OutputStream> stream;
  private final Provider<String> formatProvider;
  private final Set<String> environmentBlacklist;

  @AssistedInject
  public GCloudCommandImpl(
    final ConfigService configService,
    @Named(GCloudFormatSupplier.KEY) Provider<String> formatProvider,
    @Assisted dev.chux.gcp.crun.model.GCloudCommand gcloudCommand
  ) {
    this(configService, formatProvider, gcloudCommand, null);
  }

  @AssistedInject
  public GCloudCommandImpl(
    final ConfigService configService,
    @Named(GCloudFormatSupplier.KEY) Provider<String> formatProvider,
    @Assisted dev.chux.gcp.crun.model.GCloudCommand gcloudCommand,
    @Assisted OutputStream stream
  ) {
    this.gcloudBinary = this.gcloudBinary(configService);
    this.formatProvider = formatProvider;
    this.gcloudCommand = gcloudCommand;
    this.stream = fromNullable(stream);
    this.environmentBlacklist = this.environmentBlacklist(configService);
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
      .setVerbosity(builder)
      .setLogHttp(builder)
      .setFlag(builder, "quiet")
      .setArguments(builder)
      .addOutput(builder);

    return builder;
  } 

  private final String gcloudBinary(
    final ConfigService configService
  ) {
    return configService
      .getOptionalAppProp(PROPERTY_GCLOUD_COMMAND)
      .or(DEFAULT_GCLOUD_COMMAND);
  }

  private final Set<String> environmentBlacklist(
    final ConfigService configService
  ) {
    return ImmutableSet.copyOf(environmentBlacklistProperty(configService));
  }

  private final List<String> environmentBlacklistProperty(
    final ConfigService configService
  ) {
    return configService.getMultivalueAppProp(PROPERTY_GCLOUD_ENVIRONMENT_BLACKLIST);
  }

  private final String defaultFormat() {
    return this.formatProvider.get();
  }

  private final ManagedProcessBuilder newGCloudCommandBuilder() throws ManagedProcessException {
    return new ManagedProcessBuilder(this.gcloudBinary);
  }

  private final void checkAndSetNormalizedEnvVar(
    final Map<String, String> env,
    final String name, final String value
  ) {
    final String safeVarName = name.toUpperCase();
    if (this.environmentBlacklist.contains(safeVarName)) {
      logger.error("skipped blacklisted env var: '{}={}'", name, value);
    } else {
      env.put(safeVarName, value);
    }
  }

  private final void setEnvVar(
    final Map<String, String> env,
    final String name, final String value
  ) {
    if (!isNullOrEmpty(name) && !isNullOrEmpty(value)) {
      this.checkAndSetNormalizedEnvVar(env, name, value);
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

    this.checkAndSetNormalizedEnvVar(env, "CLOUDSDK_CORE_DISABLE_PROMPTS", "1");

    final Map<String, String> environment = this.gcloudCommand.environment();
    for (final Map.Entry<String, String> variable : environment.entrySet()) {
      this.setEnvVar(env, variable);
    }

    return this;
  }

  private final GCloudCommandImpl setFlag(
    final ManagedProcessBuilder builder,
    final String name
  ) {
    if (!isNullOrEmpty(name)) {
      builder.addArgument("--" + name);
    }
    return this;
  }

  private final GCloudCommandImpl setFlag(
    final ManagedProcessBuilder builder,
    final String name,
    final String value
  ) {
    if (isNullOrEmpty(value)) {
      this.setFlag(builder, name);
    } else if (!isNullOrEmpty(name)) {
      builder.addArgument("--" + name + "=" + value);
    }
    return this;
  }

  private final GCloudCommandImpl addOutput(final ManagedProcessBuilder builder) {
    if (this.stream.isPresent()) {
      builder.addStdOut(this.stream.get());
      builder.addStdErr(this.stream.get());
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

  private final GCloudCommandImpl setOptionalStringFlag(
    final ManagedProcessBuilder builder,
    final String name,
    final Optional<String> value
  ) {
    if (value.isPresent()) {
      return this.setFlag(builder, name, value.get());
    }
    return this;
  }

  private final GCloudCommandImpl setBooleanFlag(
    final ManagedProcessBuilder builder,
    final String name,
    final boolean isEnabled
  ) {
    if (isEnabled) {
      return this.setFlag(builder, name);
    }
    return this;
  }

  private final GCloudCommandImpl setProject(final ManagedProcessBuilder builder) {
    return this.setOptionalStringFlag(builder, "project", this.gcloudCommand.optionalProject());
  }

  private final GCloudCommandImpl setVerbosity(final ManagedProcessBuilder builder) {
    return this.setOptionalStringFlag(builder, "verbosity", this.gcloudCommand.optionalVerbosity());
  }

  private final GCloudCommandImpl setLogHttp(final ManagedProcessBuilder builder) {
    return this.setBooleanFlag(builder, "log-http", this.gcloudCommand.logHttp());
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
