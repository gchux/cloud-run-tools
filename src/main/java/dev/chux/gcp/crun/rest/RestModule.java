package dev.chux.gcp.crun.rest;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import dev.chux.gcp.crun.rest.routes.RoutesModule;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import jakarta.inject.Qualifier;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class RestModule extends AbstractModule {

    @Qualifier
    @Target({ FIELD, PARAMETER, METHOD })
    @Retention(RUNTIME)
    public @interface RunJMeterTestRoute {}

    protected void configure() {
      install(new RoutesModule());
      bind(RestAPI.class).in(Scopes.SINGLETON);
    }

}
