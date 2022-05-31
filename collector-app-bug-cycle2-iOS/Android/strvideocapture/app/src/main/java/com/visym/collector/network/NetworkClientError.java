package com.visym.collector.network;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import retrofit2.Response;

public class NetworkClientError extends RuntimeException {

    /**
     * Represents any errors returning from the backend
     */
    public static final int ERROR_TYPE_API = -2;

    /**
     * Represents any network related errors
     */
    public static final int ERROR_TYPE_NETWORK = -1;

    public static final int ERROR_TYPE_CLIENT = 1;

    /**
     * Represent any unknown errors
     */
    public static final int ERROR_TYPE_UNKNOWN = 0;

    private final int type;

    private int errorCode;

    private final Response retrofitResponse;

    private final String retrofitErrorBody;

    private final JSONObject errorsRootJsonObject;

    public NetworkClientError(Throwable throwable) {
        super(throwable);

        retrofitResponse = null;
        retrofitErrorBody = "";

        if (throwable instanceof IOException) {
            type = ERROR_TYPE_NETWORK;
            errorsRootJsonObject = null;
        } else {
            type = ERROR_TYPE_UNKNOWN;
            errorsRootJsonObject = null;
        }
    }

    public NetworkClientError(Response retrofitResponse) {
        this.retrofitResponse = retrofitResponse;

        if (retrofitResponse != null) {
            type = ERROR_TYPE_API;
            errorCode = retrofitResponse.code();
            retrofitErrorBody = extractRetrofitErrorBody(retrofitResponse);
            errorsRootJsonObject = parseRetrofitErrorResponse(retrofitErrorBody);
        }else {
            errorCode = -1;
            type = ERROR_TYPE_UNKNOWN;
            errorsRootJsonObject = null;
            retrofitErrorBody = "";
        }
    }

    /**
     * Returns the type of error, on of: {@link NetworkClientError#ERROR_TYPE_API},
     * {@link NetworkClientError#ERROR_TYPE_NETWORK}, {@link NetworkClientError#ERROR_TYPE_UNKNOWN}
     *
     * @return error type
     */
    public int getType() {
        return type;
    }

    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Returns raw retrofit response
     *
     * @return retrofit response
     */
    public Response getRetrofitResponse() {
        return retrofitResponse;
    }

    /**
     * Returns raw retrofit response error body
     *
     * @return retrofit response error body
     */
    public String getRetrofitErrorBody() {
        return retrofitErrorBody;
    }

    /**
     * Returns the raw error body from response
     * @param retrofitResponse Response returned from the retrofit request
     * @return error response body returned from the retrofit request
     */
    private String extractRetrofitErrorBody(final Response retrofitResponse) {
        try {
            return retrofitResponse.errorBody().string();
        } catch (Exception e) {
            return "";
        }
    }

    private JSONObject parseRetrofitErrorResponse(final String retrofitErrorBody) {
        if (TextUtils.isEmpty(retrofitErrorBody)) {
            return null;
        }

        try {
            final JSONObject jsonObject = new JSONObject(retrofitErrorBody);
            // TODO extract the error response from json string
//            if (jsonObject.has("errors")) {
//                return jsonObject.getJSONObject("errors");
//            }
        } catch (JSONException e) {
            //ignore
        }
        return null;
    }

    public String getErrorMessage() {
        return null;
    }
}
