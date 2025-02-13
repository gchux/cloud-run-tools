package dev.chux.gcp.crun.faults.binary;

import java.io.OutputStream;

import com.google.common.base.Optional;

import ch.vorburger.exec.ManagedProcessBuilder;

import dev.chux.gcp.crun.ConfigService;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;

public abstract class AbstractBinary<T> implements Binary<T> {

  private final String binary;
  private final Optional<String> flagSeparator;

  protected AbstractBinary(
    final ConfigService configService,
    final String binary
  ) {
    this.binary = this.getBinary(configService, binary);
    this.flagSeparator = this.getFlagSeparator(configService, binary);
  }

  @Override()
  public String get() {
    return this.binary;
  }

  private final String getFlagSeparatorKey(final String binary) {
    return "faults.binary." + binary + ".flags.separator";
  }

  private final String getBinaryKey(final String binary) {
    return "faults.binary." + binary + ".command";
  }

  private final String getBinary(
    final ConfigService configService,
    final String binary
  ) {
    final String key = getBinaryKey(binary);
    final String bin = configService.getAppProp(key);
    checkState(!isNullOrEmpty(bin), "not found:", key);
    return bin;
  }

  private final Optional<String> getFlagSeparator(
    final ConfigService configService,
    final String binary
  ) {
    final String key = getFlagSeparatorKey(binary);
    return configService.getOptionalAppProp(key);
  }

  private final String flagSeparator() {
    return this.flagSeparator.or(" ");
  }

  private final String newFlag(
    final String name,
    final String value
  ) {
    return name + this.flagSeparator() + value;
  }

  protected final ManagedProcessBuilder addFlag(
      final ManagedProcessBuilder builder,
    final String name,
    final String value
  ) {
    builder.addArgument(newFlag(name, value));
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

}
