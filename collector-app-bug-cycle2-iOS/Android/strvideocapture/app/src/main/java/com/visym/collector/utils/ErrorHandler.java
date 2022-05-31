package com.visym.collector.utils;

import com.visym.collector.network.NetworkClientError;

public class ErrorHandler {

    public static String getErrorMessage(NetworkClientError error) {
        int type = error.getType();
        if (type == NetworkClientError.ERROR_TYPE_UNKNOWN) {

        }else if (type == NetworkClientError.ERROR_TYPE_NETWORK) {
            return "Oops!... Failed to connect server. Please try again";
        }

        int code = error.getErrorCode();
        switch (code) {
            case 403:
                return "You are not authorised to access this content";

            default:
                return error.getErrorMessage();
        }
    }

}
