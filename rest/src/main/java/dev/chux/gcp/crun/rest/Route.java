package dev.chux.gcp.crun.rest;

import com.google.common.base.Supplier;

public interface Route extends spark.Route {

  public String endpoint(final String basePath);

  public void register(final String basePath);

}
