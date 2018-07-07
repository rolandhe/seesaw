package com.github.rolandhe.seesaw;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

import com.github.rolandhe.seesaw.callback.BodyProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author rolandhe
 */
class ServerWriteContext extends Context {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerWriteContext.class);
  final ByteBuffer[] writeBuffers;

  ServerWriteContext(ByteBuffer[] writeBuffers, AsynchronousSocketChannel channel, long timeout,
      long maxLive, long startTime, BodyProcessor bodyProcessor) {
    super(channel, timeout, startTime, bodyProcessor, maxLive);
    this.writeBuffers = writeBuffers;

  }

  <T extends ServerWriteContext> boolean write(CompletionHandler<Long, T> handler) {
    int offset = calStartIndex();
    try {
      channel.write(writeBuffers, offset, writeBuffers.length - offset, readTimeout,
          TimeUnit.MILLISECONDS, (T) this, handler);
    }catch (RuntimeException e) {
      LOGGER.info("WriteContext write error.", e);
      return false;
    }
    return true;
  }

  private int calStartIndex() {
    for (int i = 0; i < writeBuffers.length; i++) {
      if (writeBuffers[i].hasRemaining()) {
        return i;
      }
    }
    throw new RuntimeException();
  }

  boolean hasRemaining() {
    return writeBuffers[writeBuffers.length - 1].hasRemaining();
  }
}
