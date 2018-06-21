package com.github.rolandhe.seesaw;

import com.github.rolandhe.seesaw.callback.BodyProcessor;

import java.nio.channels.AsynchronousSocketChannel;

/**
 * @author hexiufeng
 * @date 2018/6/12下午7:44
 */
class Context {
  final AsynchronousSocketChannel channel;
  final long readTimeout;
  volatile long startTime;
  final BodyProcessor bodyProcessor;
  final long maxLive;

   Context(AsynchronousSocketChannel channel, long readTimeout, long startTime,
      BodyProcessor bodyProcessor, long maxLive) {
    this.channel = channel;
    this.readTimeout = readTimeout;
    this.startTime = startTime;
    this.bodyProcessor = bodyProcessor;
    this.maxLive = maxLive;
  }

  void checkStartTime() {
    if (startTime == -1L) {
      startTime = System.currentTimeMillis();
    }
  }
}
