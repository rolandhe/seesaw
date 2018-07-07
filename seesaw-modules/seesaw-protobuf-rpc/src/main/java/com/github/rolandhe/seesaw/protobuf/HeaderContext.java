package com.github.rolandhe.seesaw.protobuf;

import java.util.Map;

/**
 * Created by rolandhe on 2018/7/7.
 */
public interface HeaderContext {

     Map<String, String> getInHeader() ;

     Map<String, String> getOutHeader() ;
}
