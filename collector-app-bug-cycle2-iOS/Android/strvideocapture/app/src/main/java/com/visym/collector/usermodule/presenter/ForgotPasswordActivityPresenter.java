package com.visym.collector.usermodule.presenter;

import android.content.Context;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.results.ForgotPasswordResult;
import com.amazonaws.services.cognitoidentityprovider.model.UserNotFoundException;
import com.visym.collector.R;
import com.visym.collector.network.ConnectivityReceiver;
import com.visym.collector.network.IErrorInterator;
import com.visym.collector.usermodule.IUserModule.IForgotPasswordPresenter;
import com.visym.collector.usermodule.IUserModule.IForgotPasswordView;
import com.visym.collector.utils.Globals;


public class ForgotPasswordActivityPresenter implements IForgotPasswordPresenter, IErrorInterator {

    protected IForgotPasswordView mview;
    protected Context mcontext;


    @Override
    public void onViewAttached(IForgotPasswordView mview, Context context) {
        this.mview = mview;
        this.mcontext = context;

    }

    @Override
    public void onViewDettached() {
        this.mview = null;
    }

    @Override
    public void forgotPasswordApi(String emailAddress) {
        if (ConnectivityReceiver.isConnected()) {
            AWSMobileClient.getInstance().forgotPassword(emailAddress, new Callback<ForgotPasswordResult>() {
                @Override
                public void onResult(ForgotPasswordResult result) {
                    switch (result.getState()) {
                        case DONE:
                        case CONFIRMATION_CODE:
                            mview.onEmailSent(emailAddress);
                            break;
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (e instanceof UserNotFoundException) {
                        mview.onFailure(((UserNotFoundException) e).getErrorMessage());
                    } else {
                        mview.onFailure(e.getMessage());
                    }
                }
            });
        } else {
            mview.onFailure(mcontext.getResources().getString(R.string.noInternet));
        }
    }

    @Override
    public void dismissLoading() {
        Globals.dismissLoading();
    }

    @Override
    public void showLoading() {
        Globals.showLoading(mcontext);
    }

}
