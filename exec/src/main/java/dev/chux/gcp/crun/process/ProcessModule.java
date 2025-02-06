package dev.chux.gcp.crun.process;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import jakarta.inject.Qualifier;
import java.util.function.Consumer;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class ProcessModule extends AbstractModule {

  @Qualifier
  @Target({ FIELD, PARAMETER, METHOD })
  @Retention(RUNTIME)
  public @interface ProcessConsumer {}

  protected void configure() {
    install(new FactoryModuleBuilder()
        .implement(ProcessOutput.class, ProcessOutputSink.class)
        .build(ProcessOutputFactory.class));

    bind(new TypeLiteral<Consumer<ProcessProvider>>(){})
      .annotatedWith(ProcessConsumer.class)
      .to(ProcessExecutor.class)
      .in(Scopes.SINGLETON);

    bind(new TypeLiteral<Consumer<ManagedProcessProvider>>(){})
      .annotatedWith(ProcessConsumer.class)
      .to(ManagedProcessExecutor.class)
      .in(Scopes.SINGLETON);
  }

}
