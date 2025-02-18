package dev.chux.gcp.crun.faults.command;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import dev.chux.gcp.crun.model.GoogleAPIsHttpRequest;
import dev.chux.gcp.crun.model.HttpRequest;

public class CommandModule extends AbstractModule {

  public static final String NAMESPACE = "faults://commands";

  protected void configure() {
    final FactoryModuleBuilder builder = new FactoryModuleBuilder();

    final TypeLiteral<FaultCommand<HttpRequest>> httpRequestCommandType = new TypeLiteral<FaultCommand<HttpRequest>>() {};
    builder.implement(
      httpRequestCommandType,
      Names.named(HttpRequestCommand.KEY),
      HttpRequestCommand.class
    );

    final TypeLiteral<FaultCommand<GoogleAPIsHttpRequest>> gapisHttpRequestCommandType = new TypeLiteral<FaultCommand<GoogleAPIsHttpRequest>>() {};
    builder.implement(
      gapisHttpRequestCommandType,
      Names.named(GoogleAPIsHttpRequestCommand.KEY),
      GoogleAPIsHttpRequestCommand.class
    );

    install(builder.build(FaultCommandFactory.class));
  }

}
