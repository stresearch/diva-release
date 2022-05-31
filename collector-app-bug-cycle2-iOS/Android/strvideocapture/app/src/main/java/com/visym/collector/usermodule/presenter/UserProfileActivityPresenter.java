package com.visym.collector.usermodule.presenter;

import android.content.Context;
import android.util.Log;

import com.amazonaws.amplify.generated.graphql.SubjectByStrSubjectEmailQuery;
import com.amazonaws.amplify.generated.graphql.UpdateStrCollectorMutation;
import com.amazonaws.amplify.generated.graphql.UpdateStrSubjectMutation;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.SignOutOptions;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.google.android.material.snackbar.Snackbar;
import com.visym.collector.R;
import com.visym.collector.network.ConnectivityReceiver;
import com.visym.collector.network.IErrorInterator;
import com.visym.collector.network.NetworkClientError;
import com.visym.collector.usermodule.IUserModule.IUserProfilePresenter;
import com.visym.collector.usermodule.IUserModule.IUserProfileView;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.Globals;
import com.visym.collector.utils.SharedPref;

import javax.annotation.Nonnull;

import type.ModelStringKeyConditionInput;
import type.UpdateStrCollectorInput;
import type.UpdateStrSubjectInput;

import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;

public class UserProfileActivityPresenter implements IUserProfilePresenter, IErrorInterator {

    private IUserProfileView mview;
    private Context mcontext;

    private AppSharedPreference preference;

    private GraphQLCall.Callback<UpdateStrCollectorMutation.Data> updateIsConsented = new GraphQLCall.Callback<UpdateStrCollectorMutation.Data>() {
        @Override
        public void onResponse(@Nonnull Response<UpdateStrCollectorMutation.Data> response) {
            if (response != null && response.data() != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Globals.isShowingLoader()) {
                            Globals.dismissLoading();
                        }
                        mview.refreshActivity();
                    }
                });
            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e(Globals.TAG, "onFailure: " + e.toString());
        }
    };
    private GraphQLCall.Callback<UpdateStrSubjectMutation.Data> updateSubject = new GraphQLCall.Callback<UpdateStrSubjectMutation.Data>() {
        @Override
        public void onResponse(@Nonnull Response<UpdateStrSubjectMutation.Data> response) {
            if (response != null && response.data() != null && response.data().updateStrSubject() != null) {

                Log.e(Globals.TAG, "onResponse: " + response.data().updateStrSubject().status());
                preference.clearValue(Constant.IS_USER_CONSENTED);
                //preference.clearValue(Constant.COLLECTOR_EMAIL);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        UpdateStrCollectorInput collectorInput = UpdateStrCollectorInput.builder()
                                .collector_email(preference.readString(Constant.COLLECTOR_EMAIL))
                                .collector_id(preference.readString(Constant.COLLECTOR_ID_KEY))
                                .is_consented(false)
                                .build();
                        Globals.mAWSAppSyncClient.mutate(UpdateStrCollectorMutation.builder().input(collectorInput).build())
                                .enqueue(updateIsConsented);

                    }
                });
            }

        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e(Globals.TAG, "onFailure: " + e.toString());
        }
    };
    private GraphQLCall.Callback<SubjectByStrSubjectEmailQuery.Data> revokeResponse = new GraphQLCall.Callback<SubjectByStrSubjectEmailQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<SubjectByStrSubjectEmailQuery.Data> response) {
            if (response != null && response.data() != null && response.data().subjectByStrSubjectEmail() != null) {

                Log.e(Globals.TAG, "onResponse: " + response.data().subjectByStrSubjectEmail().items().get(0).collector_email());
                UpdateStrSubjectInput subjectDevInput = UpdateStrSubjectInput.builder().subject_email(preference.readString(Constant.COLLECTOR_EMAIL))
                        .collector_email(response.data().subjectByStrSubjectEmail().items().get(0).collector_email())
                        .status("inactive")
                        .build();

                Globals.mAWSAppSyncClient.mutate(UpdateStrSubjectMutation.builder().input(subjectDevInput).build())
                        .enqueue(updateSubject);
            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e(Globals.TAG, "onFailure: " + e.toString());
        }
    };

    @Override
    public void onViewAttached(IUserProfileView mview, Context context) {
        this.mview = mview;
        this.mcontext = context;
        this.preference = AppSharedPreference.getInstance();
    }

    @Override
    public void onViewDetached() {
        this.mview = null;
    }

    @Override
    public void signOutFunctionality() {
        AWSMobileClient.getInstance()
                .signOut(SignOutOptions.builder().signOutGlobally(true).build(),
                        new Callback<Void>() {
                            @Override
                            public void onResult(Void result) {
                                AppSharedPreference preference = AppSharedPreference.getInstance();
                                String collectorEmail = preference.readString(Constant.COLLECTOR_EMAIL);
                                preference.clearAll();
                                preference.storeValue(Constant.COLLECTOR_EMAIL, collectorEmail);
                                new SharedPref(mcontext).clearSharedPref();
                                mview.redirectToTheFrontScreen();
                            }

                            @Override
                            public void onError(Exception e) {
                                mview.onLogoutFailure(
                                        com.visym.collector.utils.ErrorHandler
                                                .getErrorMessage(new NetworkClientError(e.getCause())));
                            }
                        });
    }

    @Override
    public void revokeUserConsentForTheDay() {
        Globals.showLoading(mcontext);
        if (ConnectivityReceiver.isConnected()) {
            Globals.mAWSAppSyncClient.query(SubjectByStrSubjectEmailQuery.builder()
                    .subject_email(preference.readString(Constant.COLLECTOR_EMAIL))
                    .status(ModelStringKeyConditionInput.builder().eq("active").build()).build())
                    .enqueue(revokeResponse);

        } else {
            Globals
                    .showSnackBar(mcontext.getResources().getString(R.string.noInternet), mcontext,
                            Snackbar.LENGTH_SHORT);
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
