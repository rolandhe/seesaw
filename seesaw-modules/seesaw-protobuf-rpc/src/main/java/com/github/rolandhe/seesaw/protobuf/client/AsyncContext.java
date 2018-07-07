package com.github.rolandhe.seesaw.protobuf.client;

/**
 * Created by rolandhe on 2018/7/7.
 */
public class AsyncContext {

    private AsyncContext() {

    }

    private static final ThreadLocal<AsyncContext> CONTEXT_THREAD_LOCAL = new ThreadLocal<AsyncContext>(){
        protected AsyncContext initialValue() {
            return new AsyncContext();
        }
    };


    private int status;
    private String errorMessage;
    private RuntimeException exception;

    public static void clear() {
        CONTEXT_THREAD_LOCAL.get().status = -1;
        CONTEXT_THREAD_LOCAL.get().errorMessage = null;
        CONTEXT_THREAD_LOCAL.get().exception = null;
    }



    public static void putStatusInfo(int status, String errorMessage) {
        CONTEXT_THREAD_LOCAL.get().status = status;
        CONTEXT_THREAD_LOCAL.get().errorMessage = errorMessage;
    }

    public static void putException(RuntimeException exception) {
        CONTEXT_THREAD_LOCAL.get().exception = exception;
    }

    public static int getStatus() {
        return CONTEXT_THREAD_LOCAL.get().status;
    }

    public static String getErrorMessage() {
        return CONTEXT_THREAD_LOCAL.get().errorMessage;
    }

    public static RuntimeException getException() {
        return CONTEXT_THREAD_LOCAL.get().exception;
    }
}
