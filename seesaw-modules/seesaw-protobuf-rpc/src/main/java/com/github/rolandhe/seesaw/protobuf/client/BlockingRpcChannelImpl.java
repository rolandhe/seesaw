package com.github.rolandhe.seesaw.protobuf.client;

import com.github.rolandhe.seesaw.SeesawClient;
import com.github.rolandhe.seesaw.protobuf.consts.ErrorInfos;
import com.google.protobuf.BlockingRpcChannel;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;


/**
 * Created by rolandhe on 2018/7/7.
 */
public class BlockingRpcChannelImpl extends AbstractChannel implements BlockingRpcChannel {


    public BlockingRpcChannelImpl(ServiceConfProvider serviceConfProvider, SeesawClient seesawClient) {
        super(serviceConfProvider, seesawClient);
    }

    @Override
    public Message callBlockingMethod(Descriptors.MethodDescriptor method, RpcController controller, Message request, Message responsePrototype) throws ServiceException {

        RpcResponse rpcResponse =  super.callMethodSync(method,request,controller,responsePrototype);
        if(rpcResponse.status != ErrorInfos.METHOD_OK) {
            throw new RuntimeException(rpcResponse.errorMsg);
        }
        return rpcResponse.message;
    }
}
