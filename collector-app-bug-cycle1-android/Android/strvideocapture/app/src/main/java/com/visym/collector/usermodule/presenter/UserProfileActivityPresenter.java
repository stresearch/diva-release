package com.visym.collector.usermodule.presenter;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

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

import com.visym.collector.usermodule.IUserModule.IUserProfilePresenter;
import com.visym.collector.usermodule.IUserModule.IUserProfileView;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.Globals;


import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nonnull;

import type.ModelStringKeyConditionInput;
import type.UpdateStrCollectorInput;
import type.UpdateStrSubjectInput;

import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;

public class UserProfileActivityPresenter implements IUserProfilePresenter {

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
                        if (mview != null) {
                            mview.refreshActivity();
                        }
                    }
                });
            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Globals.dismissLoading();
                    Toast.makeText(mcontext, "Something went wrong. Please try again", Toast.LENGTH_SHORT)
                            .show();
                }
            });
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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Globals.dismissLoading();
                    Toast.makeText(mcontext, "Something went wrong. Please try again", Toast.LENGTH_SHORT)
                            .show();
                }
            });
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
            }else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Globals.dismissLoading();
                        Toast.makeText(mcontext, "Something went wrong. Please try again", Toast.LENGTH_SHORT)
                                .show();
                    }
                });
            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Globals.dismissLoading();
                    Toast.makeText(mcontext, "Something went wrong. Please try again", Toast.LENGTH_SHORT)
                            .show();
                }
            });
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
                                File filesDir = new File(Globals.getInstance().getFilesDir().getAbsolutePath() + "/uploads");
                                File dir = new File(Globals.getInstance().getFilesDir().getAbsolutePath() + "/Collections_Training_Videos");
                                if (filesDir.exists()){
                                    try {
                                        FileUtils.deleteDirectory(filesDir);
                                        FileUtils.deleteDirectory(dir);
                                    } catch (IOException e) {
                                        Log.d("File deletion ", "onResult: "+ e.getCause());
                                    }
                                }
                                AppSharedPreference preference = AppSharedPreference.getInstance();
                                String collectorEmail = preference.readString(Constant.COLLECTOR_EMAIL);
                                preference.clearAll();
                                preference.storeValue(Constant.COLLECTOR_EMAIL, collectorEmail);
                                if (mview != null) {
                                    mview.redirectToTheFrontScreen();
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                if (mview != null) {
                                    mview.onLogoutFailure(e.getMessage());
                                }
                            }
                        });
    }

    @Override
    public void revokeUserConsentForTheDay() {
        if (ConnectivityReceiver.isConnected()) {
            Globals.showLoading(mcontext);
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

}
