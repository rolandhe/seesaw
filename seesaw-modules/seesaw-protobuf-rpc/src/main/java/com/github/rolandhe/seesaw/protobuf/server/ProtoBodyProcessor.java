package com.github.rolandhe.seesaw.protobuf.server;

import com.github.rolandhe.seesaw.callback.BodyProcessor;
import com.github.rolandhe.seesaw.protobuf.RpcControllerWithHeaderContext;
import com.github.rolandhe.seesaw.protobuf.SeesawProtobufWrapper;
import com.github.rolandhe.seesaw.protobuf.consts.ErrorInfos;
import com.github.rolandhe.seesaw.protobuf.utils.HeaderUtil;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Created by rolandhe on 2018/6/30.
 */
public class ProtoBodyProcessor implements BodyProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtoBodyProcessor.class);

    @Override
    public ByteBuffer process(ByteBuffer bodyBuffer) throws Exception {

        SeesawProtobufWrapper.RequestPacket packet = SeesawProtobufWrapper.RequestPacket.parseFrom(ByteString.copyFrom(bodyBuffer));

        String methodName = packet.getMethodName();

        ProtobufMethod method = ServiceInstanceRegister.getMethodByFullName(methodName);
        if (method == null) {
            LOGGER.info("{} not exist.", methodName);
            return methodNotExistsResponse(methodName);
        }
        try {
            RpcControllerWithHeaderContext rpcControllerWithHeaderContext = new RpcControllerWithHeaderContext(HeaderUtil.extractHeader(packet.getHeaders()));
            Message reponseMessage = method.call(packet, rpcControllerWithHeaderContext);
            SeesawProtobufWrapper.ResponsePacket.Builder responseBuilder = SeesawProtobufWrapper.ResponsePacket.newBuilder()
                    .setStatus(ErrorInfos.METHOD_OK).setBody(reponseMessage.toByteString());
            if(rpcControllerWithHeaderContext.getOutHeader().size() > 0) {
                responseBuilder.setHeaders(HeaderUtil.convertHeaders(rpcControllerWithHeaderContext.getOutHeader()));
            }
            return ByteBuffer.wrap(responseBuilder.build().toByteArray());
        }catch (ServiceException e) {
            LOGGER.info(methodName + " run internal error.", e);
            return methodInternalErrorResponse(methodName);
        }
    }


    private ByteBuffer methodNotExistsResponse(String methodName) {
        SeesawProtobufWrapper.ResponsePacket responsePacket = SeesawProtobufWrapper.ResponsePacket.newBuilder()
                .setStatus(ErrorInfos.METHOD_NOT_EXIST)
                .setMessage(String.format(ErrorInfos.METHOD_NOT_EXIST_MESSAGE, methodName)).build();
        return ByteBuffer.wrap(responsePacket.toByteArray());
    }

    private ByteBuffer methodInternalErrorResponse(String methodName) {
        SeesawProtobufWrapper.ResponsePacket responsePacket = SeesawProtobufWrapper.ResponsePacket.newBuilder()
                .setStatus(ErrorInfos.SERVER_INTERNAL_ERROR)
                .setMessage(String.format(ErrorInfos.SERVER_INTERNAL_ERROR_MESSAGE, methodName)).build();
        return ByteBuffer.wrap(responsePacket.toByteArray());
    }
}
