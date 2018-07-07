package com.github.rolandhe.seesaw;


import com.github.rolandhe.seesaw.callback.ClientSendCallback;

/**
 * @author rolandhe
 */
public interface ClientContext {
  StateChannel getStateChannel();
  ClientSendCallback getSendCallback();
  ChannelPool getChannelPool();
}
