package com.visym.collector.usermodule.presenter;

import android.content.Context;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.results.SignUpResult;
import com.amazonaws.services.cognitoidentityprovider.model.InvalidPasswordException;
import com.amazonaws.services.cognitoidentityprovider.model.UsernameExistsException;
import com.visym.collector.usermodule.IUserModule.ISignUpPresenter;
import com.visym.collector.usermodule.IUserModule.ISignUpView;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivityPresenter implements ISignUpPresenter {

    private ISignUpView mview;

    @Override
    public void onViewAttached(ISignUpView mview, Context context) {
        this.mview = mview;
    }

    @Override
    public void onViewDetached() {
        this.mview = null;
    }

    @Override
    public void signUpApi(String firstName, String lastName, String email, String password) {
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("email", email);
        attributes.put("custom:first_name", firstName);
        attributes.put("custom:last_name", lastName);

        AWSMobileClient.getInstance().signUp(email, password, attributes, null,
                new Callback<SignUpResult>() {
                    @Override
                    public void onResult(SignUpResult result) {
                        mview.onSignUpSuccess(email, result.getConfirmationState());
                    }

                    @Override
                    public void onError(Exception e) {
                        e.printStackTrace();
                        if (e instanceof InvalidPasswordException) {
                            mview.onSignUpFailure(((InvalidPasswordException) e).getErrorMessage());
                        }
                        if (e instanceof UsernameExistsException) {
                            mview.onSignUpFailure(((UsernameExistsException) e).getErrorMessage());
                        } else {
                            mview.onSignUpFailure(e.getMessage());
                        }
                    }
                });
    }
}
