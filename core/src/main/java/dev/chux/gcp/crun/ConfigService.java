package dev.chux.gcp.crun;

public interface ConfigService {
  
  public String getEnvVar(final String name);
  public String getEnvVarOrDefault(final String name, final String defaultValue);

  public String getSysProp(final String name);
  public String getSysPropOrDefault(final String name, final String defaultValue);

}
