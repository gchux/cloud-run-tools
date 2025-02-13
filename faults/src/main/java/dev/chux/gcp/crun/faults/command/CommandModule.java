package dev.chux.gcp.crun.faults.command;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class CommandModule extends AbstractModule {

  public static final String NAMESPACE = "faults://commands";

  protected void configure() {
    final FactoryModuleBuilder builder = new FactoryModuleBuilder();

    builder.implement(
      FaultCommand.class,
      Names.named(HttpRequestCommand.NAMESPACE),
      HttpRequestCommand.class
    );

    install(builder.build(FaultCommandFactory.class));
  }

}
