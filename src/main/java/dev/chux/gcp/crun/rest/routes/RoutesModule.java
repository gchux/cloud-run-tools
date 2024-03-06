package dev.chux.gcp.crun.rest.routes;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import spark.Route;
import dev.chux.gcp.crun.rest.RestModule.RunJMeterTestRoute;

public class RoutesModule extends AbstractModule {

    protected void configure() {
      bind(Route.class)
        .annotatedWith(RunJMeterTestRoute.class)
        .to(RunJMeterTestController.class)
        .in(Scopes.SINGLETON);
    }

}
