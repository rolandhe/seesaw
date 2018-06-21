package com.github.rolandhe.seesaw;

import com.github.rolandhe.seesaw.callback.ClientSendCallback;

import java.nio.ByteBuffer;

/**
 * @author hexiufeng
 * @date 2018/6/15上午12:22
 */
class WaitContext implements ClientContext {

  final ByteBuffer lenBuffer;
  final StateChannel stateChannel;
  final long timeout;
  final long maxLive;
  final ChannelPool channelPool;

  WaitContext(ByteBuffer lenBuffer, StateChannel stateChannel, long timeout, long maxLive,
      ChannelPool channelPool) {
    this.lenBuffer = lenBuffer;
    this.stateChannel = stateChannel;
    this.timeout = timeout;
    this.maxLive = maxLive;
    this.channelPool = channelPool;
  }

  @Override
  public StateChannel getStateChannel() {
    return stateChannel;
  }

  @Override
  public ClientSendCallback getSendCallback() {
    return null;
  }

  @Override
  public ChannelPool getChannelPool() {
    return channelPool;
  }
}

