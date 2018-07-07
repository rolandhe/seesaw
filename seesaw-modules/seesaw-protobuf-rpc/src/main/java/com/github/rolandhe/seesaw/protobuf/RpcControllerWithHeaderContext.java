package com.github.rolandhe.seesaw.protobuf;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by rolandhe on 2018/7/6.
 */
public class RpcControllerWithHeaderContext implements RpcController,HeaderContext {
    private final Map<String,String> inHeader;
    private final Map<String,String> outHeader = new LinkedHashMap<>();

    public RpcControllerWithHeaderContext(Map<String,String> inHeader) {
        this.inHeader = inHeader;
    }
    @Override
    public void reset() {

    }

    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public String errorText() {
        return null;
    }

    @Override
    public void startCancel() {

    }

    @Override
    public void setFailed(String reason) {

    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void notifyOnCancel(RpcCallback<Object> callback) {

    }

    @Override
    public Map<String, String> getInHeader() {
        return inHeader;
    }

    @Override
    public Map<String, String> getOutHeader() {
        return outHeader;
    }
}
