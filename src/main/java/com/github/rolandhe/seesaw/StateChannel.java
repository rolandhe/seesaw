package com.github.rolandhe.seesaw;

import com.github.rolandhe.seesaw.callback.ClientSendCallback;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Semaphore;

/**
 * @author hexiufeng
 * @date 2018/6/11下午5:09
 */
class StateChannel {

  enum Status {
    WAITING_TIMEOUT,USING, DESTROYING;
  }
  final Semaphore readEvent = new Semaphore(1);
  final AsynchronousSocketChannel channel;
  private volatile   Status status;
  volatile  boolean isNew = true;
  volatile ClientSendCallback sendCallback;
  final String host;


  StateChannel(AsynchronousSocketChannel channel,String host) {
    this.channel = channel;
    this.host = host;
  }

   boolean isThisStatus(Status status) {
    return this.status == status;
  }

   void setStatus(Status status) {
    this.status = status;
  }

   Status getStatus() {
    return this.status;
  }

}
