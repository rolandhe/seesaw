package com.github.rolandhe.seesaw.protobuf.client;

import java.util.Arrays;
import java.util.List;

/**
 * Created by rolandhe on 2018/7/7.
 */
public class DefaultRandomServiceConfProvider implements ServiceConfProvider {
    private final List<String> hostList;

    public DefaultRandomServiceConfProvider(String hosts) {
      String[] hostArray =   hosts.split(",");
      hostList = Arrays.asList(hostArray);
    }
    @Override
    public String provide() {
        int size = hostList.size();
        int index = (int)(Math.random()*size);
        return hostList.get(index);
    }
}
