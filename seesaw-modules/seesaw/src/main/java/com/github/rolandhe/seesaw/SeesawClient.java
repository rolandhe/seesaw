package com.github.rolandhe.seesaw;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.rolandhe.seesaw.callback.ClientSendCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author rolandhe
 */
public final class SeesawClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(SeesawClient.class);

  private long connectTimout = 3000L;
  private long soTimeout = 2000L;
  private long maxLive = 30 * 1000L;

  private final ChannelPool channelPool;
  private final AsynchronousChannelGroup channelGroup;
  private final boolean createGroup;
  private final String name;


  private final ChannelPool.ChannelCreator channelCreator = new ChannelPool.ChannelCreator() {
    @Override
    public AsynchronousSocketChannel create(String host) {
      String[] array = host.split(":");
      AsynchronousSocketChannel channel = null;
      Future<Void> future = null;
      try {
        channel = AsynchronousSocketChannel
            .open(channelGroup);
        future = channel
            .connect(new InetSocketAddress(array[0], Integer.parseInt(array[1])));
        future.get(connectTimout, TimeUnit.MILLISECONDS);
        channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        channel.setOption(StandardSocketOptions.TCP_NODELAY, true);

        return channel;
      } catch (IOException e) {
        throw new RuntimeException(e);
      } catch (InterruptedException | TimeoutException | ExecutionException e) {
        if (future != null) {
          future.cancel(true);
        }
        SeesawUtils.safeClose(channel);
        throw new RuntimeException(e);
      }
    }
  };

  public SeesawClient(String name, ChannelPool channelPool, AsynchronousChannelGroup channelGroup) {
    this.name = name;
    this.channelPool = channelPool;
    this.channelGroup = channelGroup;
    this.createGroup = false;
  }

  public SeesawClient(String name, ChannelPool channelPool, int threadSize) {
    this.name = name;
    try {
      AsynchronousChannelGroup asynchronousChannelGroup = AsynchronousChannelGroup
          .withFixedThreadPool(threadSize, new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
              Thread thread = new Thread(r);
              thread.setName(name + "-thread");
              return thread;
            }
          });
      this.channelGroup = asynchronousChannelGroup;
      this.createGroup = true;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    this.channelPool = channelPool;
  }


  private static final CompletionHandler<Long, ClientWriteContext> WRITE_BODY_HANDLER = new CompletionHandler<Long, ClientWriteContext>() {

    @Override
    public void completed(Long result, ClientWriteContext attachment) {
      if (result == -1) {
        LOGGER.info("READ_LEN_HANDLER peer reset.");
        SeesawUtils.disposeChannel(attachment, true);
        return;
      }
      if (attachment.hasRemaining()) {
        attachment.write(this, true);
        return;
      }
      if (!attachment.stateChannel.isNew) {
        attachment.stateChannel.readEvent.release();
        return;
      }

      ByteBuffer lenBuffer = ByteBuffer.allocate(4);
      ClientReadContext clientReadContext = new ClientReadContext(lenBuffer, attachment.channel,
          attachment.readTimeout, attachment.maxLive, System.currentTimeMillis(),
          attachment.stateChannel, attachment.sendCallback);
      clientReadContext.channelPool = attachment.channelPool;
      clientReadContext.read(READ_LEN_HANDLER, true);
    }

    @Override
    public void failed(Throwable exc, ClientWriteContext attachment) {
      LOGGER.info("WRITE_BODY_HANDLER failed.", exc);
      SeesawUtils.disposeChannel(attachment, true);
    }
  };

  private static final CompletionHandler<Integer, ClientReadContext> READ_LEN_HANDLER = new CompletionHandler<Integer, ClientReadContext>() {

    @Override
    public void completed(Integer result, ClientReadContext attachment) {
      if (result == -1) {
        LOGGER.info("READ_LEN_HANDLER peer reset.");
        SeesawUtils.disposeChannel(attachment, true);
        return;
      }
      if (attachment.readBuffer.hasRemaining()) {
        attachment.read(this, true);
        return;
      }
      attachment.readBuffer.flip();
      int len = attachment.readBuffer.getInt();
      if (len == 0) {
        SeesawUtils.disposeChannel(attachment, true);
        return;
      }
      ByteBuffer byteBuffer = ByteBuffer.allocate(len);
      ClientReadContext readContext = new ClientReadContext(byteBuffer, attachment.channel,
          attachment.readTimeout, attachment.maxLive, attachment.startTime,
          attachment.stateChannel, attachment.sendCallback);
      readContext.channelPool = attachment.channelPool;
      readContext.read(READ_BODY_HANDLER, true);
    }

    @Override
    public void failed(Throwable exc, ClientReadContext attachment) {
      LOGGER.info("READ_LEN_HANDLER failed.", exc);
      SeesawUtils.disposeChannel(attachment, true);
    }
  };

  private static final CompletionHandler<Integer, ClientReadContext> READ_BODY_HANDLER = new CompletionHandler<Integer, ClientReadContext>() {

    @Override
    public void completed(Integer result, ClientReadContext attachment) {
      if (result == -1) {
        LOGGER.info("READ_BODY_HANDLER peer reset.");
        SeesawUtils.disposeChannel(attachment, true);
        return;
      }
      if (attachment.readBuffer.hasRemaining()) {
        attachment.checkStartTime();
        attachment.read(this, true);
        return;
      }

      attachment.readBuffer.flip();
      attachment.stateChannel.sendCallback = null;

      attachment.sendCallback.callback(attachment.readBuffer, false);

      waitWriteFinishStatus(attachment.stateChannel, result);

      attachment.stateChannel.setStatus(StateChannel.Status.WAITING_TIMEOUT);
      ByteBuffer lenBuffer = ByteBuffer.allocate(4);
      lenBuffer.order(ByteOrder.LITTLE_ENDIAN);

      try {
        attachment.channel.read(lenBuffer, attachment.maxLive, TimeUnit.MILLISECONDS,
            new WaitContext(lenBuffer, attachment.stateChannel, attachment.readTimeout,
                attachment.maxLive, attachment.channelPool),
            WAIT_HANDLER);
        attachment.channelPool.giveBack(attachment.stateChannel);
      } catch (RuntimeException e) {
        LOGGER.info("READ_BODY_HANDLER to WAIT_HANDLER", e);
        SeesawUtils.disposeChannel(attachment, true);
      }
    }

    @Override
    public void failed(Throwable exc, ClientReadContext attachment) {
      LOGGER.info("READ_BODY_HANDLER failed.", exc);
      SeesawUtils.disposeChannel(attachment, true);
    }
  };

  private static void waitWriteFinishStatus(final StateChannel stateChannel, int result) {
    if (!stateChannel.isNew) {
      while (true) {

        try {
          if (stateChannel.readEvent.tryAcquire(100, TimeUnit.MILLISECONDS)) {
            break;
          } else {
            LOGGER.info("READ_BODY_HANDLER waiting writing finish,{}.", result);
          }
        } catch (InterruptedException e) {
          // ignore
        }
      }
    }
  }

  private static final CompletionHandler<Integer, WaitContext> WAIT_HANDLER = new CompletionHandler<Integer, WaitContext>() {

    @Override
    public void completed(Integer result, final WaitContext waitContext) {
      if (result == -1) {
        LOGGER.info("WAIT_HANDLER peer reset.");
        boolean using = waitContext.stateChannel.isThisStatus(StateChannel.Status.USING);
        SeesawUtils.disposeChannel(convertWaitContext(waitContext, using), using);
        return;
      }

      if (waitContext.lenBuffer.hasRemaining()) {
        ClientReadContext clientReadContext = new ClientReadContext(waitContext.lenBuffer,
            waitContext.stateChannel.channel,
            waitContext.timeout, waitContext.maxLive, System.currentTimeMillis(),
            waitContext.stateChannel, waitContext.stateChannel.sendCallback);
        clientReadContext.channelPool = waitContext.channelPool;
        clientReadContext.read(READ_LEN_HANDLER, true);
        return;
      }

      waitContext.lenBuffer.flip();
      int len = waitContext.lenBuffer.getInt();

      ByteBuffer byteBuffer = ByteBuffer.allocate(len);
      ClientReadContext readContext = new ClientReadContext(byteBuffer,
          waitContext.stateChannel.channel,
          waitContext.timeout, waitContext.maxLive, System.currentTimeMillis(),
          waitContext.stateChannel, waitContext.stateChannel.sendCallback);
      readContext.channelPool = waitContext.channelPool;
      readContext.read(READ_BODY_HANDLER, true);
    }

    @Override
    public void failed(Throwable exc, WaitContext waitContext) {

      LOGGER.info("WAIT_HANDLER failed." + waitContext.stateChannel.channel.isOpen() + System
          .identityHashCode(waitContext.stateChannel.channel), exc);
      if (exc instanceof InterruptedByTimeoutException) {
        if (waitContext.stateChannel.isThisStatus(StateChannel.Status.USING)) {
          try {
            waitContext.stateChannel.channel
                .read(waitContext.lenBuffer, waitContext.maxLive, TimeUnit.MILLISECONDS,
                    waitContext,
                    WAIT_HANDLER);
          } catch (RuntimeException e) {
            LOGGER.info("WAIT_HANDLER  continue to read length .", exc);
            SeesawUtils.disposeChannel(convertWaitContext(waitContext, true), true);
          }
          return;
        }
      }
      boolean using = waitContext.stateChannel.isThisStatus(StateChannel.Status.USING);
      SeesawUtils.disposeChannel(waitContext, using);
    }
  };

  private static ClientContext convertWaitContext(final WaitContext waitContext,
      final boolean using) {
    return new ClientContext() {

      @Override
      public StateChannel getStateChannel() {
        return waitContext.getStateChannel();
      }

      @Override
      public ClientSendCallback getSendCallback() {
        if (!using) {
          return null;
        }
        return waitContext.stateChannel.sendCallback;
      }

      @Override
      public ChannelPool getChannelPool() {
        return waitContext.getChannelPool();
      }
    };
  }

  public void send(String host, ByteBuffer bodyBuffer, ClientSendCallback sendCallback) {
    StateChannel stateChannel = this.channelPool.borrow(host, connectTimout, this.channelCreator);
    if (stateChannel == null) {
      throw new RuntimeException();
    }
    int len = bodyBuffer.limit();
    ByteBuffer lenBuffer = ByteBuffer.allocate(4);
    lenBuffer.order(ByteOrder.LITTLE_ENDIAN);
    lenBuffer.putInt(bodyBuffer.limit());

    lenBuffer.flip();

    boolean hasNext = true;
    while (true) {
      lenBuffer.position(0);
      lenBuffer.limit(4);
      bodyBuffer.position(0);
      bodyBuffer.limit(len);
      if (!stateChannel.isNew) {
        stateChannel.sendCallback = sendCallback;
      }
      ClientWriteContext clientWriteContext = new ClientWriteContext(
          new ByteBuffer[]{lenBuffer, bodyBuffer}, stateChannel.channel, soTimeout, maxLive,
          System.currentTimeMillis(), stateChannel, sendCallback);
      clientWriteContext.channelPool = this.channelPool;
      if (!clientWriteContext.write(WRITE_BODY_HANDLER)) {
        LOGGER.info("write exp- state:{}--{}--{}", stateChannel.getStatus(),
            stateChannel.channel.isOpen(), hasNext);
        channelPool.release(stateChannel, true);
        stateChannel.sendCallback = null;
        SeesawUtils.safeClose(stateChannel.channel);
        if (!hasNext) {
          sendCallback.callback(null, true);
          break;
        }
        stateChannel = this.channelPool.borrow(host, connectTimout, this.channelCreator);
        if (stateChannel == null) {
          throw new RuntimeException();
        }
        hasNext = false;
      } else {
        break;
      }
    }
  }

  public Future<ByteBuffer> send(String host, ByteBuffer bodyBuffer) {

    final InternalFuture future = new InternalFuture();

    send(host, bodyBuffer, new ClientSendCallback() {
      @Override
      public void callback(ByteBuffer returnBody, boolean failed) {
        if (failed) {
          future.failed = true;

        } else {
          future.buffer = returnBody;
        }
        future.done = true;
        future.countDownLatch.countDown();
      }
    });

    return future;

  }

  private static class InternalFuture implements Future<ByteBuffer> {

    final CountDownLatch countDownLatch = new CountDownLatch(1);
    volatile ByteBuffer buffer;
    volatile boolean done;
    volatile boolean failed;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return false;
    }

    @Override
    public boolean isCancelled() {
      return false;
    }

    @Override
    public boolean isDone() {
      return done;
    }

    @Override
    public ByteBuffer get() throws InterruptedException, ExecutionException {
      countDownLatch.await();
      if (failed) {
        throw new ExecutionException(null);
      }
      return buffer;
    }

    @Override
    public ByteBuffer get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
      if (countDownLatch.await(timeout, unit)) {
        if (failed) {
          throw new ExecutionException(null);
        }
        return buffer;
      }
      return null;
    }
  }

  public synchronized void destory() {
    if (this.createGroup) {
      try {
        this.channelGroup.shutdownNow();
      } catch (IOException e) {
        // ignore
      } catch (RuntimeException e) {
        LOGGER.info("channelGroup.shutdownNow error", e);
      }
      try {
        this.channelGroup.awaitTermination(2000L, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        // ignore
      }
    }
  }

}
