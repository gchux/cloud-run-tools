package dev.chux.gcp.crun.process;

public interface ProcessProvider {

  public ProcessBuilder getBuilder();
  public ProcessOutput getOutput();

}
