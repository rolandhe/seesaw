package com.github.rolandhe.seesaw.protobuf.utils;

import com.github.rolandhe.seesaw.protobuf.SeesawProtobufWrapper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by rolandhe on 2018/7/7.
 */
public class HeaderUtil {

    private HeaderUtil(){}

    public static Map<String,String> extractHeader(SeesawProtobufWrapper.Headers headers) {
        if(headers == null || headers.getHeaderPairsCount() == 0) {
            return Collections.emptyMap();
        }
        Map<String,String> map = new LinkedHashMap<>();
        for(SeesawProtobufWrapper.HeaderPair headerPair : headers.getHeaderPairsList()) {
            map.put(headerPair.getName(),headerPair.getValue());
        }

        return Collections.unmodifiableMap(map);
    }

    public static SeesawProtobufWrapper.Headers convertHeaders(Map<String,String> headerMap) {

        SeesawProtobufWrapper.Headers.Builder builder = SeesawProtobufWrapper.Headers.newBuilder();
        for(Map.Entry<String,String> entry : headerMap.entrySet()) {
            builder.addHeaderPairs(SeesawProtobufWrapper.HeaderPair.newBuilder().setName(entry.getKey()).setValue(entry.getValue()));
        }
        return builder.build();
    }
}
