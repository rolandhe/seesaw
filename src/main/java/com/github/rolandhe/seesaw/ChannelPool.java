package com.github.rolandhe.seesaw;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @author hexiufeng
 * @date 2018/6/11下午5:08
 */
public final class ChannelPool {

  private int routerMax = 20;
  private long offlineReleasePeriod = 5000L;
  private long delayReleaseOfflineHostTime = 10000L;


  private final Map<String, ChannelStack> channelMap = new ConcurrentHashMap<>();
  private final BlockingQueue<DelayReleaseChannelStack> delayReleaseChannelStackBlockingDeque = new LinkedBlockingQueue<>();

  private ScheduledExecutorService scheduledExecutorService;

  public int getRouterMax() {
    return routerMax;
  }

  public void setRouterMax(int routerMax) {
    this.routerMax = routerMax;
  }

  public long getOfflineReleasePeriod() {
    return offlineReleasePeriod;
  }

  public void setOfflineReleasePeriod(long offlineReleasePeriod) {
    this.offlineReleasePeriod = offlineReleasePeriod;
  }

  public long getDelayReleaseOfflineHostTime() {
    return delayReleaseOfflineHostTime;
  }

  public void setDelayReleaseOfflineHostTime(long delayReleaseOfflineHostTime) {
    this.delayReleaseOfflineHostTime = delayReleaseOfflineHostTime;
  }


  interface ChannelCreator {

    AsynchronousSocketChannel create(String host);
  }

  static class DelayReleaseChannelStack {

    final long timeStamp = System.currentTimeMillis();
    final ChannelStack channelStack;

    DelayReleaseChannelStack(ChannelStack channelStack) {
      this.channelStack = channelStack;
    }
  }

  static class ChannelStack {

    private static final int TRY_GET_IIMES = 3;
    private final BlockingQueue<StateChannel> stack = new LinkedBlockingQueue<>();
    private final Semaphore limit;
    private final ChannelCreator channelCreator;
    private final String host;

    void clear() {
      StateChannel stateChannel = null;
      while ((stateChannel = stack.poll()) != null) {
        if (!stateChannel.isThisStatus(StateChannel.Status.DESTROYING)) {
          try {
            stateChannel.channel.close();
          } catch (IOException e) {
            // ignore
          }
        }
      }
    }

    ChannelStack(String host, int max, ChannelCreator channelCreator) {
      limit = new Semaphore(max);
      this.channelCreator = channelCreator;
      this.host = host;
    }

    StateChannel borrowChannel(long timeout) {
      boolean acquired = false;
      if (timeout == 0L) {
        acquired = limit.tryAcquire();
      } else {
        try {
          acquired = limit.tryAcquire(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
          // ignore
        }
      }
      if (acquired) {
        return tryGet(TRY_GET_IIMES);
      }
      return null;
    }

    void returnChannel(StateChannel stateChannel) {
      stateChannel.isNew = false;
      if (!stateChannel.isThisStatus(StateChannel.Status.WAITING_TIMEOUT)) {
        return;
      }

      this.stack.offer(stateChannel);
      limit.release();
    }

    StateChannel tryGet(int times) {
      for (int i = 0; i < times; i++) {
        StateChannel stateChannel = stack.poll();
        if (stateChannel == null) {
          break;
        }
        if (stateChannel.isThisStatus(StateChannel.Status.DESTROYING)) {
          continue;
        }
        stateChannel.setStatus(StateChannel.Status.USING);
        stateChannel.readEvent.tryAcquire();
        return stateChannel;
      }
      StateChannel newStateChannel = new StateChannel(channelCreator.create(host), host);
      newStateChannel.setStatus(StateChannel.Status.USING);
      newStateChannel.readEvent.tryAcquire();
      return newStateChannel;
    }

    void release(StateChannel stateChannel, boolean isUsing) {
      if (!stateChannel.isThisStatus(StateChannel.Status.DESTROYING)) {
        stateChannel.setStatus(StateChannel.Status.DESTROYING);
        if (isUsing) {
          limit.release();
        }
      }
    }
  }

  public synchronized void init() {
    if (scheduledExecutorService != null) {
      return;
    }
    this.scheduledExecutorService = Executors.newScheduledThreadPool(1);
    scheduledExecutorService.scheduleAtFixedRate(() -> {
      while (true) {
        DelayReleaseChannelStack delayReleaseChannelStack = delayReleaseChannelStackBlockingDeque
            .peek();
        if (System.currentTimeMillis() - delayReleaseChannelStack.timeStamp
            >= getDelayReleaseOfflineHostTime()) {
          delayReleaseChannelStack.channelStack.clear();
          delayReleaseChannelStackBlockingDeque.poll();
        } else {
          break;
        }
      }
    }, getOfflineReleasePeriod(), getOfflineReleasePeriod(), TimeUnit.MILLISECONDS);
  }

  public synchronized void destroy() {
    if (scheduledExecutorService == null) {
      return;
    }

    scheduledExecutorService.shutdown();

    try {
      scheduledExecutorService.awaitTermination(5000L, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      // ignore
    }

    scheduledExecutorService = null;

    for (Map.Entry<String, ChannelStack> entry : channelMap.entrySet()) {
      entry.getValue().clear();
    }
  }

  public StateChannel borrow(String host, long timeout, ChannelCreator channelCreator) {
    ChannelStack channelStack = channelMap.get(host);
    if (channelStack == null) {
      channelStack = new ChannelStack(host, getRouterMax(), channelCreator);
      ChannelStack old = channelMap.putIfAbsent(host, channelStack);
      if (old != null) {
        channelStack = old;
      }
    }
    return channelStack.borrowChannel(timeout);
  }


  public void giveBack(StateChannel stateChannel) {
    ChannelStack channelStack = channelMap.get(stateChannel.host);
    if (channelStack == null) {
      return;
    }
    channelStack.returnChannel(stateChannel);
  }

  public void release(StateChannel stateChannel, boolean isUsing) {
    ChannelStack channelStack = channelMap.get(stateChannel.host);
    if (channelStack == null) {
      return;
    }
    channelStack.release(stateChannel, isUsing);

  }


  public void offline(String host) {
    ChannelStack channelStack = channelMap.get(host);
    if (channelStack != null) {
      channelMap.remove(host);
      synchronized (this) {
        if (scheduledExecutorService != null) {
          this.delayReleaseChannelStackBlockingDeque
              .offer(new DelayReleaseChannelStack(channelStack));
        }
      }
    }
  }
}
