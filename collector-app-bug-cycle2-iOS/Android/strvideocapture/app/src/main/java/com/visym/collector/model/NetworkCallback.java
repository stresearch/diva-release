package com.visym.collector.model;

import com.visym.collector.network.NetworkClientError;

public interface NetworkCallback<T> {

    void onSuccess(T response);

    void onFailure(NetworkClientError error);
}
