package com.github.rolandhe.seesaw.protobuf.server;

import com.github.rolandhe.seesaw.protobuf.RpcControllerWithHeaderContext;
import com.github.rolandhe.seesaw.protobuf.SeesawProtobufWrapper;
import com.google.protobuf.BlockingService;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import com.google.protobuf.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by rolandhe on 2018/6/30.
 */
class ServiceMethod implements ProtobufMethod {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceMethod.class);
    private final BlockingService service;
    private final Descriptors.MethodDescriptor methodDescriptor;

    ServiceMethod(BlockingService service, Descriptors.MethodDescriptor methodDescriptor) {
        this.service = service;
        this.methodDescriptor = methodDescriptor;
    }

    @Override
    public Message call(SeesawProtobufWrapper.RequestPacket requestPacket, RpcControllerWithHeaderContext rpcControllerWithHeaderContext) throws ServiceException {
        Parser<? extends Message> parser = service.getRequestPrototype(methodDescriptor).getParserForType();
        Message request = null;
        try {
            request = parser.parseFrom(requestPacket.getBody());
        } catch (InvalidProtocolBufferException e) {
            LOGGER.info("parse error.", e);
            throw new ServiceException("parse error.", e);
        }
        return service.callBlockingMethod(methodDescriptor, rpcControllerWithHeaderContext, request);
    }
}
