package com.github.rolandhe.seesaw;

import com.github.rolandhe.seesaw.callback.ClientSendCallback;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * @author hexiufeng
 * @date 2018/6/14下午5:41
 */
class ClientReadContext extends ServerReadContext implements ClientContext {

  final StateChannel stateChannel;
  final ClientSendCallback sendCallback;

  volatile ChannelPool channelPool;

  ClientReadContext(ByteBuffer readBuffer, AsynchronousSocketChannel channel,
      long readTimeout,
      long maxLive, long startTime, StateChannel stateChannel,
      ClientSendCallback sendCallback) {
    super(readBuffer, channel, readTimeout, maxLive, startTime, null);
    this.stateChannel = stateChannel;
    this.sendCallback = sendCallback;
  }

  <T extends ServerReadContext> boolean read(CompletionHandler<Integer, T> handler,boolean isUsing) {
    boolean isRead = super.read(handler);
    if(!isRead) {
      AsinkUtils.disposeChannel(this,isUsing);
    }
    return  isRead;
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
