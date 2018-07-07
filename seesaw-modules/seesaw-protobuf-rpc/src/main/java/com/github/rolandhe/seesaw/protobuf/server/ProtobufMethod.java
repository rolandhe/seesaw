package com.github.rolandhe.seesaw.protobuf.server;

import com.github.rolandhe.seesaw.protobuf.RpcControllerWithHeaderContext;
import com.github.rolandhe.seesaw.protobuf.SeesawProtobufWrapper;
import com.google.protobuf.Message;
import com.google.protobuf.ServiceException;


/**
 * Created by rolandhe on 2018/6/30.
 */
public interface ProtobufMethod {

    Message call(SeesawProtobufWrapper.RequestPacket requestPacket,RpcControllerWithHeaderContext rpcControllerWithHeaderContext) throws ServiceException;
}
