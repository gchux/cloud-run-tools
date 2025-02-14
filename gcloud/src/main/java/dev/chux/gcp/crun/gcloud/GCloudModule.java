package dev.chux.gcp.crun.gcloud;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import dev.chux.gcp.crun.gcloud.rest.RestModule;

public class GCloudModule extends AbstractModule {

  public static final String NAMESPACE = "gcloud://cli";

  protected void configure() {
    bind(GCloudFormatSupplier.class).in(Scopes.SINGLETON);
    bind(String.class)
      .annotatedWith(Names.named(GCloudFormatSupplier.KEY))
      .toProvider(GCloudFormatSupplier.class);

    final FactoryModuleBuilder builder = new FactoryModuleBuilder();

    builder.implement(GCloudCommand.class, GCloudCommandImpl.class);

    install(builder.build(GCloudCommandFactory.class));

    bind(GCloudService.class).in(Scopes.SINGLETON);

    install(new RestModule());
  }

}
