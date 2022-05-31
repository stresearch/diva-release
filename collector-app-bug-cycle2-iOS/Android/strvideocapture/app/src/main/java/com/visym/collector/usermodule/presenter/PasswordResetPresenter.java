package com.visym.collector.usermodule.presenter;

import android.content.Context;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.results.ForgotPasswordResult;
import com.amazonaws.services.cognitoidentityprovider.model.InvalidPasswordException;
import com.amazonaws.services.cognitoidentityprovider.model.UserNotFoundException;
import com.visym.collector.R;
import com.visym.collector.network.ConnectivityReceiver;
import com.visym.collector.usermodule.IUserModule;

public class PasswordResetPresenter implements IUserModule.IPasswordResetPresenter {

    private final Context context;
    private final IUserModule.IPasswordResetView mView;

    public PasswordResetPresenter(Context context, IUserModule.IPasswordResetView mView) {
        this.context = context;
        this.mView = mView;
    }

    @Override
    public void resendOTP(String email) {
        if (ConnectivityReceiver.isConnected()) {
            AWSMobileClient.getInstance().forgotPassword(email, new Callback<ForgotPasswordResult>() {
                @Override
                public void onResult(ForgotPasswordResult result) {
                    switch (result.getState()) {
                        case DONE:
                            mView.onEmailSent(email);
                            break;
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (e instanceof UserNotFoundException) {
                        mView.onFailure(((UserNotFoundException) e).getErrorMessage());
                    } else {
                        mView.onFailure(e.getMessage());
                    }
                }
            });
        } else {
            mView.onFailure(context.getResources().getString(R.string.noInternet));
        }
    }

    @Override
    public void resetPassword(String otpText, String newPass) {
        if (ConnectivityReceiver.isConnected()) {
            AWSMobileClient.getInstance().confirmForgotPassword(newPass, otpText, new Callback<ForgotPasswordResult>() {
                @Override
                public void onResult(ForgotPasswordResult result) {
                    switch (result.getState()) {
                        case DONE:
                            mView.onResetPasswordSuccess();
                            break;
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (e instanceof InvalidPasswordException) {
                        mView.onFailure(((InvalidPasswordException) e).getErrorMessage());
                    } else {
                        mView.onFailure(e.getMessage());
                    }
                }
            });
        } else {
            mView.onFailure(context.getResources().getString(R.string.noInternet));
        }
    }
}
