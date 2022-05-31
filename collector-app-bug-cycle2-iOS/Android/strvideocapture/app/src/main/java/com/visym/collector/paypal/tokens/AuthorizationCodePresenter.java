/**
 * @file SessionExpireDetailsPresenter.java
 * @brief This is the presenter class for movie details functionality, it will act as
 * an intermediate between views and model
 * @author Shrikant
 * @date 15/04/2018
 */

package com.visym.collector.paypal.tokens;


import android.content.Context;

public class AuthorizationCodePresenter implements AuthorizationCodeContract.Presenter, AuthorizationCodeContract.Model.OnFinishedListener {

    private AuthorizationCodeContract.View view;
    private AuthorizationCodeContract.Model model;

    public AuthorizationCodePresenter(AuthorizationCodeContract.View view) {
        this.view = view;
        this.model = new AuthorizationCodeModel();
    }

    @Override
    public void onTokenDestroy() {

        view = null;
    }




    @Override
    public void requestTokenData(String authCode_refreshToken,String granttype,String key, Context context) {

        if(view != null){
            view.showTokenProgress();
        }
        model.getTokenDetails(this, authCode_refreshToken,granttype,key, context);
    }

    @Override
    public void onTokenFinished(AuthorizationCodeDataModel data) {

        if(view != null){
            view.hideTokenProgress();
        }
        view.onResponseTokenSuccess(data);

    }

    @Override
    public void onTokenFailure(Throwable t, AuthorizationCodeDataModel data) {
        if(view != null){
            view.hideTokenProgress();
        }
        view.onResponseTokenFailure(t,data);
    }
}
