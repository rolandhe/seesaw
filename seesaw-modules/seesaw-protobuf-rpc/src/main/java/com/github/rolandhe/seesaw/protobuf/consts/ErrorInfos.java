package com.github.rolandhe.seesaw.protobuf.consts;

/**
 * Created by rolandhe on 2018/7/6.
 */
public interface ErrorInfos {
    int METHOD_OK = 200;
    int METHOD_NOT_EXIST = 404;
    String METHOD_NOT_EXIST_MESSAGE = "% method is not exist.";
    int SERVER_INTERNAL_ERROR = 500;

    String SERVER_INTERNAL_ERROR_MESSAGE = "call % method occur internal error.";

    int NETWORK_EXP = 1;

    int PROTOCOL_ERROR = 2;
}
