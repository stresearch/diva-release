/**
 * @file SessionExpireDetailsContract.java
 * @brief This is the contract class for Movie details MVP
 * @author Shrikant
 * @date 15/04/2018
 */
package com.visym.collector.paypal.tokens;


import android.content.Context;

public interface AuthorizationCodeContract {

    interface Model {

        interface OnFinishedListener {
            void onTokenFinished(AuthorizationCodeDataModel data);

            void onTokenFailure(Throwable t, AuthorizationCodeDataModel data);
        }

        void getTokenDetails(OnFinishedListener onFinishedListener, String authCode_refreshToken,String granttype ,String key,Context context);
    }

    interface View {

        void showTokenProgress();

        void hideTokenProgress();

        void onResponseTokenSuccess(AuthorizationCodeDataModel data);

        void onResponseTokenFailure(Throwable throwable, AuthorizationCodeDataModel data);



    }

    interface Presenter {
        void onTokenDestroy();

        void requestTokenData(String authCode_refreshToken,String granttype,String key , Context context);
    }
}
