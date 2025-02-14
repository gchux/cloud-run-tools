package dev.chux.gcp.crun.process;

final class ManagedProcessExecution {

  private final ManagedProcessProvider provider;
  private final Integer exitCode;

  ManagedProcessExecution(
    final ManagedProcessProvider provider,
    final Integer exitCode
  ) {
    this.provider = provider;
    this.exitCode = exitCode;
  }

  Integer exitCode() {
    return this.exitCode;
  }

  ManagedProcessProvider provider() {
    return this.provider;
  }

}
