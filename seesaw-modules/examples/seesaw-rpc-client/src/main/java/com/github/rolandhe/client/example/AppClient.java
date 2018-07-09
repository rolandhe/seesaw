package com.github.rolandhe.client.example;

import com.github.rolandhe.seesaw.ChannelPool;
import com.github.rolandhe.seesaw.SeesawClient;
import com.github.rolandhe.seesaw.protobuf.Sample;
import com.github.rolandhe.seesaw.protobuf.client.BlockingRpcChannelImpl;
import com.github.rolandhe.seesaw.protobuf.client.ServiceConfProvider;
import com.google.protobuf.ServiceException;

/**
 * Created by rolandhe on 2018/7/9.
 */
public class AppClient {
    public static void main(String[] args) throws ServiceException {
        ChannelPool channelPool = new ChannelPool();
        channelPool.init();
        SeesawClient seesawClient = new SeesawClient("seesaw-client", channelPool, 8);
        BlockingRpcChannelImpl blockingRpcChannel = new BlockingRpcChannelImpl(new ServiceConfProvider() {
            @Override
            public String provide() {
                return "localhost:9001";
            }
        }, seesawClient);

        Sample.FooService.BlockingInterface blockingInterface = Sample.FooService.newBlockingStub(blockingRpcChannel);
        Sample.FooRequest request = Sample.FooRequest.newBuilder().setId(1001L).setName("jack").build();
        Sample.FooResponse response = blockingInterface.doRequest(null, request);

        System.out.println(response.getStatus());
        System.out.println(response.getMessage());

        System.out.println("to destroy resources");
        seesawClient.destroy();
        channelPool.destroy();

    }
}
