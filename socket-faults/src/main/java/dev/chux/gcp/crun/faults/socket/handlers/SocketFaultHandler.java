package dev.chux.gcp.crun.faults.socket.handlers;

import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import com.google.common.base.Supplier;

import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Observer;

public interface SocketFaultHandler extends Supplier<String>, ObservableOnSubscribe<Socket>, Observer<Socket> {

  public Boolean start();
  public Boolean stop(final CountDownLatch stopSignal);
  public Boolean isActive();

}

