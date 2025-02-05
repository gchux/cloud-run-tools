package dev.chux.gcp.crun.rest;

import com.google.inject.AbstractModule;

public class RestModule extends AbstractModule {

    protected void configure() {
      bind(RestAPI.class).asEagerSingleton();
    }

}
