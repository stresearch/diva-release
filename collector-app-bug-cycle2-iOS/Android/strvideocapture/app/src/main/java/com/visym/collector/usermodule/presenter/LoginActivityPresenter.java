package com.visym.collector.usermodule.presenter;

import android.content.Context;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.results.SignInResult;
import com.amazonaws.mobile.client.results.SignInState;
import com.amazonaws.services.cognitoidentityprovider.model.InvalidPasswordException;
import com.amazonaws.services.cognitoidentityprovider.model.NotAuthorizedException;
import com.amazonaws.services.cognitoidentityprovider.model.PasswordResetRequiredException;
import com.amazonaws.services.cognitoidentityprovider.model.UserNotConfirmedException;
import com.amazonaws.services.cognitoidentityprovider.model.UserNotFoundException;
import com.visym.collector.network.IErrorInterator;
import com.visym.collector.usermodule.IUserModule.ILoginPresenter;
import com.visym.collector.usermodule.IUserModule.ILoginView;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.Globals;

import java.util.Map;


public class LoginActivityPresenter implements ILoginPresenter, IErrorInterator {

    private ILoginView mview;
    private Context mcontext;

    @Override
    public void onViewAttached(ILoginView mview, Context context) {
        this.mview = mview;
        this.mcontext = context;
    }

    @Override
    public void onViewDetached() {
        this.mview = null;
    }

    @Override
    public void loginApi(String username, String password) {
        AWSMobileClient.getInstance().signIn(username, password, null, new Callback<SignInResult>() {
            @Override
            public void onResult(SignInResult result) {
                if (result.getSignInState() == SignInState.DONE) {
                    try {
                        Map<String, String> userAttributes = AWSMobileClient.getInstance().getUserAttributes();
                        if (userAttributes != null) {
                            try {
                                String userId = userAttributes.get("sub");
                                String firstName = userAttributes.get("custom:first_name");
                                String lastName = userAttributes.get("custom:last_name");
                                String email = userAttributes.get("email");
                                AppSharedPreference.getInstance().storeValue(Constant.COLLECTOR_ID_KEY, userId);
                                AppSharedPreference.getInstance().storeValue(Constant.COLLECTOR_FNAME, firstName);
                                AppSharedPreference.getInstance().storeValue(Constant.COLLECTOR_LNAME, lastName);
                                AppSharedPreference.getInstance().storeValue(Constant.COLLECTOR_EMAIL, email);
                            } catch (Exception e) {

                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mview.redirectUserSetupActivity();
                }
            }

            @Override
            public void onError(Exception e) {
                if (e instanceof InvalidPasswordException) {
                    mview.onRequestFailure(((InvalidPasswordException) e).getErrorMessage());
                } else if (e instanceof UserNotConfirmedException) {
                    mview.redirectEmailVerificationActivity(username);
                } else if (e instanceof NotAuthorizedException) {
                    mview.onRequestFailure(((NotAuthorizedException) e).getErrorMessage());
                } else if (e instanceof UserNotFoundException) {
                    mview.onRequestFailure(((UserNotFoundException) e).getErrorMessage());
                } else if (e instanceof PasswordResetRequiredException) {
                    mview.onRequestFailure(((PasswordResetRequiredException) e).getErrorMessage());
                    mview.redirectToResetPassword(username);
                } else {
                    mview.onRequestFailure(e.getMessage());
                }
            }
        });
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
