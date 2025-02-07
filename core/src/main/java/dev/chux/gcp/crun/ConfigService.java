package dev.chux.gcp.crun;

public interface ConfigService {
  
  public String getEnvVar(final String name);
  public String getEnvVarOrDefault(final String name, final String defaultValue);

  public String getAppProp(final String name);
  public String getAppPropOrDefault(final String name, final String defaultValue);

}
