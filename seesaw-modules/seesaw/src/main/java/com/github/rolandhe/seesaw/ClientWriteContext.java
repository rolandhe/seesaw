package com.github.rolandhe.seesaw;

import com.github.rolandhe.seesaw.callback.ClientSendCallback;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * @author rolandhe
 */
class ClientWriteContext extends ServerWriteContext implements  ClientContext{


  final StateChannel stateChannel;
  final ClientSendCallback sendCallback;
  volatile ChannelPool channelPool;

  ClientWriteContext(ByteBuffer[] writeBuffers, AsynchronousSocketChannel channel,
      long timeout,
      long maxLive, long startTime, StateChannel stateChannel, ClientSendCallback sendCallback) {
    super(writeBuffers, channel, timeout, maxLive, startTime, null);
    this.stateChannel = stateChannel;
    this.sendCallback = sendCallback;
  }

  <T extends ServerWriteContext> boolean write(CompletionHandler<Long, T> handler,boolean isUsing) {
    boolean isWrited = super.write(handler);
    if(!isWrited) {
      SeesawUtils.disposeChannel(this,isUsing);
    }
    return isWrited;
  }

  @Override
  public StateChannel getStateChannel() {
    return this.stateChannel;
  }

  @Override
  public ClientSendCallback getSendCallback() {
    return this.sendCallback;
  }

  @Override
  public ChannelPool getChannelPool() {
    return this.channelPool;
  }
}