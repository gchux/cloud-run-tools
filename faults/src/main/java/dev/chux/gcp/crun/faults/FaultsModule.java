package dev.chux.gcp.crun.faults;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

import dev.chux.gcp.crun.faults.binary.BinaryModule;
import dev.chux.gcp.crun.faults.command.CommandModule;
import dev.chux.gcp.crun.faults.rest.RestModule;

public class FaultsModule extends AbstractModule {

  protected void configure() {
    install(new CommandModule());

    install(new BinaryModule());

    install(new RestModule());

    bind(FaultsService.class).in(Scopes.SINGLETON);
  }

}
