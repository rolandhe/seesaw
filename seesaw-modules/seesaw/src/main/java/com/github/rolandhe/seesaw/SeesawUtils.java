package com.github.rolandhe.seesaw;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author rolandhe
 */
public class SeesawUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(SeesawUtils.class);
  private SeesawUtils() {

  }

  public static void safeClose(AsynchronousSocketChannel channel) {
    if (channel == null) {
      return;
    }
    try {
      if (channel.isOpen()) {
        channel.close();
      }
    } catch (IOException e) {
      LOGGER.info("close error.", e);
    }
  }
  public static void disposeChannel(ClientContext clientContext, boolean isUsing) {
    clientContext.getStateChannel().sendCallback = null;
    clientContext.getChannelPool().release(clientContext.getStateChannel(),isUsing);
    safeClose(clientContext.getStateChannel().channel);
    if(isUsing && clientContext.getSendCallback() != null) {
      clientContext.getSendCallback().callback(null, true);
    }
  }
}
