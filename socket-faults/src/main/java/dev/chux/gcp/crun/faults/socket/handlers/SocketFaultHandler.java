package dev.chux.gcp.crun.faults.socket.handlers;

import java.net.Socket;

import com.google.common.base.Supplier;

import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Observer;

public interface SocketFaultHandler extends Supplier<String>, ObservableOnSubscribe<Socket>, Observer<Socket> {

  public void start();
  public boolean stop();
  public boolean isActive();

}

