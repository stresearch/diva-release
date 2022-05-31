/**
 * @file SessionExpireDetailsPresenter.java
 * @brief This is the presenter class for movie details functionality, it will act as
 * an intermediate between views and model
 * @author Shrikant
 * @date 15/04/2018
 */

package com.visym.collector.paypal.identitys;


import android.content.Context;

public class IdentityPresenter implements IdentityContract.Presenter, IdentityContract.Model.OnFinishedListener {

    private IdentityContract.View view;
    private IdentityContract.Model model;

    public IdentityPresenter(IdentityContract.View view) {
        this.view = view;
        this.model = new IdentityModel();
    }

    @Override
    public void onIdentityDestroy() {

        view = null;
    }




    @Override
    public void requestIdentityData(String accesstoken, Context context) {

        if(view != null){
            view.showIdentityProgress();
        }
        model.getIdentityDetails(this, accesstoken, context);
    }

    @Override
    public void onIdentityFinished(IdentityDataModel data) {

        if(view != null){
            view.hideIdentityProgress();
        }
        view.onResponseIdentitySuccess(data);

    }

    @Override
    public void onIdentityFailure(Throwable t, IdentityDataModel data) {
        if(view != null){
            view.hideIdentityProgress();
        }
        view.onResponseIdentityFailure(t,data);
    }
}
