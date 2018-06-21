package com.github.rolandhe.seesaw;


import com.github.rolandhe.seesaw.callback.ClientSendCallback;

/**
 * @author hexiufeng
 * @date 2018/6/14下午5:53
 */
public interface ClientContext {
  StateChannel getStateChannel();
  ClientSendCallback getSendCallback();
  ChannelPool getChannelPool();
}
