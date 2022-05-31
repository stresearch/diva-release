/**
 * @file SessionExpireDetailsContract.java
 * @brief This is the contract class for Movie details MVP
 * @author Shrikant
 * @date 15/04/2018
 */
package com.visym.collector.paypal.identitys;


import android.content.Context;

public interface IdentityContract {

    interface Model {

        interface OnFinishedListener {
            void onIdentityFinished(IdentityDataModel data);

            void onIdentityFailure(Throwable t, IdentityDataModel data);
        }

        void getIdentityDetails(OnFinishedListener onFinishedListener, String accesstoken, Context context);
    }

    interface View {

        void showIdentityProgress();

        void hideIdentityProgress();

        void onResponseIdentitySuccess(IdentityDataModel data);

        void onResponseIdentityFailure(Throwable throwable, IdentityDataModel data);



    }

    interface Presenter {
        void onIdentityDestroy();

        void requestIdentityData(String accessttoken, Context context);
    }
}
