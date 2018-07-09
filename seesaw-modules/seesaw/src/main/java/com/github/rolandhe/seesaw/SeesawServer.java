package com.github.rolandhe.seesaw;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

import com.github.rolandhe.seesaw.callback.BodyProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author rolandhe
 */
public final class SeesawServer {
  private static final Logger LOGGER = LoggerFactory.getLogger(SeesawServer.class);

  private String name = "Seesaw-Server";
  private long timeout = 5000L;
  private long maxLive = 600L * 1000L;
  private int port = 5000;
  private int threadCount = 128;

  private AsynchronousChannelGroup threadGroup;
  private AsynchronousServerSocketChannel currentListener;

  public SeesawServer(String serverName,int port, int threadCount) {
    this.name = serverName;
    this.port = port;
    this.threadCount = threadCount;
  }


  private static final CompletionHandler<Integer, ServerReadContext> READ_LEN_HANDLER = new CompletionHandler<Integer, ServerReadContext>() {

    @Override
    public void completed(Integer result, ServerReadContext attachment) {
      if (result == -1) {
        LOGGER.info("READ_LEN_HANDLER peer reset.");
        safeClose(attachment.channel);
        return;
      }
      if (attachment.readBuffer.hasRemaining()) {
        if(!attachment.read(this)) {
          safeClose(attachment.channel);
        }
        return;
      }
      attachment.readBuffer.flip();
      int len = attachment.readBuffer.getInt();
      if (len == 0) {
        safeClose(attachment.channel);
        return;
      }
      ByteBuffer byteBuffer = ByteBuffer.allocate(len);
      ServerReadContext readContext = new ServerReadContext(byteBuffer, attachment.channel,
          attachment.readTimeout, attachment.maxLive, attachment.startTime,
          attachment.bodyProcessor);
      readContext.read(READ_BODY_HANDLER);
    }

    @Override
    public void failed(Throwable exc, ServerReadContext attachment) {
      LOGGER.info("READ_LEN_HANDLER failed.", exc);
      safeClose(attachment.channel);
    }
  };


  private static final CompletionHandler<Integer, ServerReadContext> READ_BODY_HANDLER = new CompletionHandler<Integer, ServerReadContext>() {

    @Override
    public void completed(Integer result, ServerReadContext attachment) {
      if (result == -1) {
        LOGGER.info("READ_BODY_HANDLER peer reset.");
        safeClose(attachment.channel);
        return;
      }

      if (attachment.readBuffer.hasRemaining()) {
        attachment.checkStartTime();
        if(!attachment.read(this)) {
          safeClose(attachment.channel);
        }
        return;
      }
      attachment.readBuffer.flip();
      attachment.checkStartTime();
      ByteBuffer byteBuffer = null;
      try {
         byteBuffer = attachment.bodyProcessor.process(attachment.readBuffer);
      } catch (Exception e) {
        LOGGER.info(null, e);
        safeClose(attachment.channel);
        return;
      }
      writeResult(byteBuffer, attachment);
    }

    @Override
    public void failed(Throwable exc, ServerReadContext attachment) {
      LOGGER.info("READ_BODY_HANDLER failed.",exc);
      safeClose(attachment.channel);
    }
  };

  private static final CompletionHandler<Long, ServerWriteContext> WRITE_BODY_HANDLER = new CompletionHandler<Long, ServerWriteContext>() {

    @Override
    public void completed(Long result, ServerWriteContext attachment) {
      if(result == -1) {
        LOGGER.info("WRITE_BODY_HANDLER peer reset");
        safeClose(attachment.channel);
        return;
      }
      if (attachment.hasRemaining()) {
        if(!attachment.write(this)){
          safeClose(attachment.channel);
        }
        return;
      }
      handle(attachment.channel, attachment.bodyProcessor, attachment.readTimeout,
          attachment.maxLive,
          true);
    }

    @Override
    public void failed(Throwable exc, ServerWriteContext attachment) {
      LOGGER.info("WRITE_BODY_HANDLER failed.",exc);
      safeClose(attachment.channel);
    }
  };


  private static void writeResult(ByteBuffer byteBuffer, ServerReadContext readContext) {
    if (byteBuffer.position() > 0) {
      byteBuffer.flip();
    }
    int len = byteBuffer.limit();
    ByteBuffer lenBuffer = ByteBuffer.allocate(4);
    lenBuffer.order(ByteOrder.LITTLE_ENDIAN);
    lenBuffer.putInt(len);
    lenBuffer.flip();
    ServerWriteContext writeContext = new ServerWriteContext(new ByteBuffer[]{lenBuffer, byteBuffer},
        readContext.channel,
        readContext.readTimeout, readContext.maxLive, readContext.startTime,
        readContext.bodyProcessor);
    if(!writeContext.write(WRITE_BODY_HANDLER)) {
      safeClose(readContext.channel);
    }
  }


  private static void safeClose(AsynchronousSocketChannel channel) {
    try {
      channel.close();
    } catch (IOException e) {
      // ignore
    }
  }

  public void start(final BodyProcessor bodyProcessor) throws IOException {

    AsynchronousChannelGroup group = AsynchronousChannelGroup
        .withFixedThreadPool(threadCount, (Runnable r) -> {
              Thread thread = new Thread(r);
              thread.setName(name);
              return thread;
            }
        );
    final AsynchronousServerSocketChannel listener =
        AsynchronousServerSocketChannel.open(group).bind(new InetSocketAddress(port));
    listener.setOption(StandardSocketOptions.SO_REUSEADDR, true);

    listener.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
      public void completed(AsynchronousSocketChannel ch, Void att) {
        listener.accept(null, this);

        handle(ch, bodyProcessor, getTimeout(), getMaxLive(), false);
      }

      public void failed(Throwable exc, Void att) {
        LOGGER.info("accept error.", exc);
      }
    });

    this.currentListener = listener;
    this.threadGroup = group;

  }


  public void shutDown() {
    this.threadGroup.shutdown();
    try {
      threadGroup.awaitTermination(5000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    try {
      this.currentListener.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  private static void handle(AsynchronousSocketChannel ch, BodyProcessor bodyProcessor,
      long timeout, long live, boolean wait) {
    ByteBuffer lenBuf = ByteBuffer.allocate(4);
    ServerReadContext readContext = new ServerReadContext(
        lenBuf, ch, wait ? live : timeout, live,
        wait ? -1L : System.currentTimeMillis(),
        bodyProcessor);

    if(!readContext.read(READ_LEN_HANDLER)) {
      safeClose(ch);
    }
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  public long getMaxLive() {
    return maxLive;
  }

  public void setMaxLive(long maxLive) {
    this.maxLive = maxLive;
  }
}
