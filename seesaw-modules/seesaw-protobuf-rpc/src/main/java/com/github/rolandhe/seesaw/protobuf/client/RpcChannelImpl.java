package com.github.rolandhe.seesaw.protobuf.client;

import com.github.rolandhe.seesaw.SeesawClient;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;

/**
 * Created by rolandhe on 2018/7/7.
 */
public class RpcChannelImpl extends AbstractChannel implements RpcChannel {
    protected RpcChannelImpl(ServiceConfProvider serviceConfProvider, SeesawClient seesawClient) {
        super(serviceConfProvider, seesawClient);
    }

    @Override
    public void callMethod(Descriptors.MethodDescriptor method, RpcController controller, Message request, Message responsePrototype, RpcCallback<Message> done) {
        super.callMethod(method, request, controller, responsePrototype, done);
    }
}
