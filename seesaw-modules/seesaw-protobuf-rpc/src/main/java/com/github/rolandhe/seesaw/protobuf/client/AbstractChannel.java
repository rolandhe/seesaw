package com.github.rolandhe.seesaw.protobuf.client;

import com.github.rolandhe.seesaw.SeesawClient;
import com.github.rolandhe.seesaw.callback.ClientSendCallback;
import com.github.rolandhe.seesaw.protobuf.HeaderContext;
import com.github.rolandhe.seesaw.protobuf.SeesawProtobufWrapper;
import com.github.rolandhe.seesaw.protobuf.consts.ErrorInfos;
import com.github.rolandhe.seesaw.protobuf.utils.HeaderUtil;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

/**
 * Created by rolandhe on 2018/7/7.
 */
public abstract class AbstractChannel {
    protected final ServiceConfProvider serviceConfProvider;
    private final SeesawClient seesawClient;

    protected static class RpcResponse{
        final int status;
        final String errorMsg;
        Message message;
        RpcResponse(int status, String errorMsg) {
            this.status = status;
            this.errorMsg = errorMsg;
        }
    }

    protected AbstractChannel(ServiceConfProvider serviceConfProvider,SeesawClient seesawClient) {
        this.serviceConfProvider = serviceConfProvider;
        this.seesawClient = seesawClient;
    }



    protected RpcResponse callMethodSync(Descriptors.MethodDescriptor method,Message request, RpcController controller,Message responsePrototype) {
        HeaderContext headerContext = null;
        if (controller != null && (controller instanceof HeaderContext)) {
            headerContext = (HeaderContext) controller;
        }
        byte[] buffer =  packetRequest(method,headerContext,request);
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        ByteBuffer responseBuffer = null;
        try {
            responseBuffer = seesawClient.send(serviceConfProvider.provide(),byteBuffer).get();
            return parseResponse(responseBuffer,headerContext,responsePrototype);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    protected void callMethod(Descriptors.MethodDescriptor method,Message request, RpcController controller,final Message responsePrototype,RpcCallback<Message> done) {
        HeaderContext headerContext = null;
        if (controller != null && (controller instanceof HeaderContext)) {
            headerContext = (HeaderContext) controller;
        }

        final HeaderContext hc = headerContext;
        byte[] buffer =  packetRequest(method,headerContext,request);
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);

        seesawClient.send(serviceConfProvider.provide(),byteBuffer,new ClientSendCallback() {

            @Override
            public void callback(ByteBuffer returnBody, boolean failed) {
                if(failed) {
                    AsyncContext.putStatusInfo(ErrorInfos.NETWORK_EXP,"network error");
                    callRpcCallback(null,done);
                    return;
                }
                try{
                    RpcResponse rpcResponse = parseResponse(returnBody,hc,responsePrototype);
                    AsyncContext.putStatusInfo(rpcResponse.status,rpcResponse.errorMsg);
                    callRpcCallback(rpcResponse.message,done);
                }catch (RuntimeException e) {
                    AsyncContext.putStatusInfo(ErrorInfos.PROTOCOL_ERROR,"parser protocol error");
                    AsyncContext.putException(e);
                    callRpcCallback(null,done);
                }
            }
        });

    }

    private void callRpcCallback(Message message,RpcCallback<Message> done) {
        try {
            done.run(null);
        }finally {
            AsyncContext.clear();
        }
    }

    private RpcResponse parseResponse(ByteBuffer responseBuffer,HeaderContext headerContext,Message responsePrototype){
        try {
            SeesawProtobufWrapper.ResponsePacket responsePacket = SeesawProtobufWrapper.ResponsePacket.parseFrom(responseBuffer.array());
            RpcResponse rpcResponse = new RpcResponse(responsePacket.getStatus(),responsePacket.getMessage());
            rpcResponse.message = responsePrototype.getParserForType().parseFrom(responsePacket.getBody());
            if(headerContext != null) {
                headerContext.getOutHeader().putAll(HeaderUtil.extractHeader(responsePacket.getHeaders()));
            }
            return rpcResponse;
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }



    private byte[] packetRequest(Descriptors.MethodDescriptor method, HeaderContext headerContext, Message request) {
        String methodName = method.getFullName();
        SeesawProtobufWrapper.RequestPacket.Builder builder = SeesawProtobufWrapper.RequestPacket.newBuilder().setBody(request.toByteString())
                .setMethodName(methodName);
        if (headerContext != null && headerContext.getInHeader() != null && headerContext.getInHeader().size() > 0) {
            SeesawProtobufWrapper.Headers headers = HeaderUtil.convertHeaders(headerContext.getInHeader());
            builder.setHeaders(headers);
        }
        return builder.setProtocol(SeesawProtobufWrapper.ProtocolType.BIN).build().toByteArray();
    }
}
