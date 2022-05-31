package com.visym.collector.network;

public class ServerResponse<T> {
    String message;
    boolean success;
    String code;
    String Error;
    T data;
    Integer count;

    public T getData() {
        return data;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Integer getCount() {
        return count;
    }

    public String getCode() {
        return code;
    }

    public String getError() {
        return Error;
    }
}
