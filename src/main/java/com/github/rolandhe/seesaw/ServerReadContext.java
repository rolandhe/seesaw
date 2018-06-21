package com.github.rolandhe.seesaw;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

import com.github.rolandhe.seesaw.callback.BodyProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author rolandhe
 */
 class ServerReadContext extends Context {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerReadContext.class);
  final ByteBuffer readBuffer;

  ServerReadContext(ByteBuffer readBuffer, AsynchronousSocketChannel channel, long readTimeout,
      long maxLive,
      long startTime, BodyProcessor bodyProcessor) {
    super(channel, readTimeout, startTime, bodyProcessor, maxLive);
    this.readBuffer = readBuffer;
    readBuffer.order(ByteOrder.LITTLE_ENDIAN);
  }

 <T extends ServerReadContext> boolean read(CompletionHandler<Integer, T> handler) {
    try {

      channel.read(readBuffer, readTimeout, TimeUnit.MILLISECONDS, (T)this, handler);
    }catch (RuntimeException e) {
      LOGGER.info("ReadContext read error.",e);
      return false;
    }
    return true;
  }
}




