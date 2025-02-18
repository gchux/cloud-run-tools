package dev.chux.gcp.crun.faults.binary;

import java.io.OutputStream;

import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;

import ch.vorburger.exec.ManagedProcessBuilder;
import ch.vorburger.exec.ManagedProcessException;

import dev.chux.gcp.crun.ConfigService;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

public abstract class AbstractBinary<T> implements Binary<T> {

  private final String propertiesPrefix;

  private final String binary;
  private final List<String> shortFlags;
  private final Optional<String> flagSeparator;

  protected AbstractBinary(
    final ConfigService configService,
    final String binary
  ) {
    this.propertiesPrefix = this.getPropertiesPrefix(binary);
    this.binary = this.getBinary(configService, this.propertiesPrefix);
    this.shortFlags = this.getShortFlags(configService, this.propertiesPrefix);
    this.flagSeparator = this.getFlagSeparator(configService, this.propertiesPrefix);
  }

  @Override()
  public String get() {
    return this.binary;
  }

  private final String getPropertiesPrefix(final String binary) {
    return "faults.binary." + binary;
  }

  private final String getFlagsKey(final String prefix) {
    return prefix + ".flags";
  }

  private final String getShortFlagsKey(final String prefix) {
    return this.getFlagsKey(prefix) + ".short";
  }

  private final String getFlagSeparatorKey(final String prefix) {
    return this.getFlagsKey(prefix) + ".separator";
  }

  private final String getBinaryKey(final String prefix) {
    return prefix + ".command";
  }

  private final String getBinary(
    final ConfigService configService,
    final String propertiesPrefix
  ) {
    final String key = getBinaryKey(propertiesPrefix);
    final String bin = configService.getAppProp(key);
    checkState(!isNullOrEmpty(bin), "not found:", key);
    return bin;
  }

  private final Optional<String> getFlagSeparator(
    final ConfigService configService,
    final String propertiesPrefix
  ) {
    final String key = getFlagSeparatorKey(propertiesPrefix);
    return configService.getOptionalAppProp(key);
  }

  private final List<String> getShortFlags(
    final ConfigService configService,
    final String propertiesPrefix
  ) {
    final String key = getShortFlagsKey(propertiesPrefix);
    return configService.getMultivalueAppProp(key);
  }

  private final String flagSeparator() {
    return this.flagSeparator.or(" ");
  }

  private final String newLongFlag(
    final String name,
    final String value
  ) {
    return "--" + name + this.flagSeparator() + value;
  }

  private final ManagedProcessBuilder setShortFlags(final ManagedProcessBuilder builder) {
    for(final String shortFlag : this.shortFlags) {
      builder.addArgument("-" + shortFlag);
    }
    return builder;
  }

  protected final ManagedProcessBuilder newCommandBuilder() throws ManagedProcessException {
    final ManagedProcessBuilder builder = new ManagedProcessBuilder(this.binary);
    return this.setShortFlags(builder);
  }

  protected final ManagedProcessBuilder addLongFlag(
      final ManagedProcessBuilder builder,
    final String name,
    final String value
  ) {
    builder.addArgument(newLongFlag(name, value), false);
    return builder;
  }

  protected final AbstractBinary addArgument(
    final ManagedProcessBuilder builder,
    final String value
  ) {
    builder.addArgument(value);
    return this;
  }

  protected final AbstractBinary addStdOut(
    final ManagedProcessBuilder builder,
    final Optional<OutputStream> stream
  ) {
    if (stream.isPresent()) {
      builder.addStdOut(stream.get());
    }
    return this;
  }
  
  protected final AbstractBinary addStdErr(
    final ManagedProcessBuilder builder,
    final Optional<OutputStream> stream
  ) {
    if (stream.isPresent()) {
      builder.addStdErr(stream.get());
    }
    return this;
  }

  protected final AbstractBinary setEnvVar(
    final Map<String, String> container,
    final String name, final String value
  ) {
    container.put(name, value);
    return this;
  }

  protected final AbstractBinary appendEnvVar(
    final Map<String, String> container,
    final String name,
    final String currentValue,
    final String value,
    final String separator
  ) {
    if (isNullOrEmpty(value)) {
      // no-op
      return this;
    }

    if (isNullOrEmpty(currentValue)) {
      // if there's no current value:
      //   - this operation is reducible to `setEnvVar` with `value`
      return this.setEnvVar(container, name, value);
    }
    
    container.put(name, currentValue + separator + value);

    return this;
  }

  protected final AbstractBinary setEnvVar(
    final ManagedProcessBuilder builder,
    final String name, final String value
  ) {
    return this.setEnvVar(builder.getEnvironment(), name, value);
  }

  protected final AbstractBinary appendEnvVar(
    final ManagedProcessBuilder builder,
    final String name,
    final String value,
    final String separator
  ) {
    final Map<String, String> env = builder.getEnvironment();
    
    final String currentValue = env.get(name);

    if (isNullOrEmpty(currentValue)) {
      return this.setEnvVar(env, name, value);
    }

    return this.appendEnvVar(env, name, currentValue, value, separator);
  }

}
