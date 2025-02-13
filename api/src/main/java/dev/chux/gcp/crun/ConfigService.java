package dev.chux.gcp.crun;

import java.util.List;

import com.google.common.base.Optional;

public interface ConfigService {
  
  public String getEnvVar(final String name);
  public List<String> getMultivalueEnvVar(final String name);
  public Optional<Integer> getIntEnvVar(final String name);
  public Optional<Long> getLongEnvVar(final String name);
  public Optional<Double> getDoubleEnvVar(final String name);
  public Optional<String> getOptionalEnvVar(final String name);
  public Optional<Boolean> getBooleanEnvVar(final String name);
  public String getEnvVarOrDefault(final String name, final String defaultValue);

  public String getAppProp(final String name);
  public List<String> getMultivalueAppProp(final String name);
  public Optional<Integer> getIntAppProp(final String name);
  public Optional<Long> getLongAppProp(final String name);
  public Optional<Double> getDoubleAppProp(final String name);
  public Optional<Boolean> getBooleanAppProp(final String name);
  public Optional<String> getOptionalAppProp(final String name);
  public String getAppPropOrDefault(final String name, final String defaultValue);

}
