package com.github.rolandhe.server.example;

import com.github.rolandhe.seesaw.SeesawServer;
import com.github.rolandhe.seesaw.protobuf.server.ProtoBodyProcessor;
import com.github.rolandhe.seesaw.protobuf.server.ServiceInstanceRegister;

import java.io.IOException;

/**
 * Created by rolandhe on 2018/7/9.
 */
public class AppServer {
    public static void main(String[] args) throws IOException {
        ServiceInstanceRegister.registerServiceInstance(BlockingInterfaceImpl.class,new BlockingInterfaceImpl());
        SeesawServer seesawServer = new SeesawServer("seesaw-server",9001,16);
        seesawServer.start(new ProtoBodyProcessor());
    }
}
