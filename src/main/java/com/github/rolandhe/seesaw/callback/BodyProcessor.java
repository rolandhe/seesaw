package com.github.rolandhe.seesaw.callback;

import java.nio.ByteBuffer;

/**
 * @author hexiufeng
 * @date 2018/6/8下午5:50
 */
public interface BodyProcessor {
  ByteBuffer process(ByteBuffer bodyBuffer) throws  Exception;
}
