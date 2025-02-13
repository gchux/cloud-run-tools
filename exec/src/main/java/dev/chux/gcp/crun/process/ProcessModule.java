package dev.chux.gcp.crun.process;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.util.function.Consumer;
import jakarta.inject.Qualifier;

import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import com.google.common.base.Function;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class ProcessModule extends AbstractModule {

  @Target({ FIELD, PARAMETER, METHOD })
  @Retention(RUNTIME)
  @Qualifier @BindingAnnotation
  public @interface ProcessConsumer {}

  @Target({ FIELD, PARAMETER, METHOD })
  @Retention(RUNTIME)
  @Qualifier @BindingAnnotation
  public @interface MultiProcessConsumer {}

  protected void configure() {
    install(new FactoryModuleBuilder()
        .implement(ProcessOutput.class, ProcessOutputSink.class)
        .build(ProcessOutputFactory.class));

    bind(new TypeLiteral<Consumer<ProcessProvider>>(){})
      .annotatedWith(ProcessConsumer.class)
      .to(ProcessExecutor.class)
      .in(Scopes.SINGLETON);

    bind(ManagedProcessExecutor.class).in(Scopes.SINGLETON);

    bind(new TypeLiteral<Consumer<ManagedProcessProvider>>(){})
      .annotatedWith(ProcessConsumer.class)
      .to(ManagedProcessExecutor.class)
      .in(Scopes.SINGLETON);

    bind(new TypeLiteral<Function<ManagedProcessProvider, Integer>>(){})
      .annotatedWith(ProcessConsumer.class)
      .to(ManagedProcessExecutor.class)
      .in(Scopes.SINGLETON);

    bind(ManagedMultiProcessExecutor.class).in(Scopes.SINGLETON);

    bind(new TypeLiteral<Consumer<ManagedMultiProcessProvider>>(){})
      .annotatedWith(ProcessConsumer.class)
      .to(ManagedMultiProcessExecutor.class)
      .in(Scopes.SINGLETON);
  }

}
