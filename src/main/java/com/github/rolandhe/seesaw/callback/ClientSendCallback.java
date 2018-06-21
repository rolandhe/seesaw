package com.github.rolandhe.seesaw.callback;

import java.nio.ByteBuffer;

/**
 * @author hexiufeng
 * @date 2018/6/15上午12:19
 */
public interface ClientSendCallback {
  void callback(ByteBuffer returnBody, boolean failed);
}
