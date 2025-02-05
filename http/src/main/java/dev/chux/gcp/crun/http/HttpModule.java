package dev.chux.gcp.crun.http;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class HttpModule extends AbstractModule {

    protected void configure() {
      bind(HttpServer.class).in(Scopes.SINGLETON);
    }

}
