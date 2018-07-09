package com.github.rolandhe.server.example;

import com.github.rolandhe.seesaw.protobuf.Sample;
import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;

/**
 * Created by rolandhe on 2018/7/9.
 */
public class BlockingInterfaceImpl implements Sample.FooService.BlockingInterface {
    @Override
    public Sample.FooResponse doRequest(RpcController controller, Sample.FooRequest request) throws ServiceException {
        return Sample.FooResponse.newBuilder().setStatus(true).setMessage(request.getName()).build();
    }
}
