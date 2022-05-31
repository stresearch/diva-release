package com.visym.collector.model;

public interface NetworkCallback<T> {

    void onSuccess(T response);

    void onFailure(String error);
}
