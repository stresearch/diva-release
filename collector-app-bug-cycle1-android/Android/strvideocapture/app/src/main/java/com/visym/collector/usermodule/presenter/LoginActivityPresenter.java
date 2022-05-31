package com.visym.collector.usermodule.presenter;

import android.content.Context;
import android.widget.Toast;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.results.SignInResult;
import com.amazonaws.mobile.client.results.SignInState;
import com.amazonaws.services.cognitoidentityprovider.model.InvalidPasswordException;
import com.amazonaws.services.cognitoidentityprovider.model.NotAuthorizedException;
import com.amazonaws.services.cognitoidentityprovider.model.PasswordResetRequiredException;
import com.amazonaws.services.cognitoidentityprovider.model.UserNotConfirmedException;
import com.amazonaws.services.cognitoidentityprovider.model.UserNotFoundException;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.visym.collector.R;
import com.visym.collector.capturemodule.views.VideoCaptureActivity;
import com.visym.collector.dashboardmodule.view.DashboardActivity;
import com.visym.collector.usermodule.IUserModule.ILoginPresenter;
import com.visym.collector.usermodule.IUserModule.ILoginView;
import com.visym.collector.usermodule.view.FrontScreenActivity;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.Globals;
import android.content.Intent;

import java.util.Map;


public class LoginActivityPresenter implements ILoginPresenter {

    private ILoginView mview;
    private Context mcontext;

    @Override
    public void onViewAttached(ILoginView mview, Context context) {
        this.mview = mview;
        this.mcontext = context;
    }

    // JEBYRNE: common function to clear user state on error
    private void logout() {
        //Toast.makeText(this.mcontext, "Network Error! Restarting...").show();  // cannot do this outside of main UI thread
        FirebaseCrashlytics.getInstance().setUserId("");
        FirebaseCrashlytics.getInstance().setCustomKey("collector_id", "");
        AppSharedPreference.getInstance().storeValue(Constant.COLLECTOR_ID_KEY, "");
        AppSharedPreference.getInstance().storeValue(Constant.COLLECTOR_FNAME, "");
        AppSharedPreference.getInstance().storeValue(Constant.COLLECTOR_LNAME, "");
        AppSharedPreference.getInstance().storeValue(Constant.COLLECTOR_EMAIL, "");
        Intent loginIntent = new Intent(this.mcontext, FrontScreenActivity.class);
        this.mcontext.startActivity(loginIntent);  // JEBYRNE: TESTING
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
                                FirebaseCrashlytics.getInstance().setUserId(userId);
                                FirebaseCrashlytics.getInstance().setCustomKey("collector_id", userId);
                                AppSharedPreference.getInstance().storeValue(Constant.COLLECTOR_ID_KEY, userId);
                                AppSharedPreference.getInstance().storeValue(Constant.COLLECTOR_FNAME, firstName);
                                AppSharedPreference.getInstance().storeValue(Constant.COLLECTOR_LNAME, lastName);
                                AppSharedPreference.getInstance().storeValue(Constant.COLLECTOR_EMAIL, email);
                                mview.redirectUserSetupActivity();

                            } catch (Exception e) {
                                logout();  // JEBYRNE: clear state on exception
                            }
                        }
                    } catch (Exception e) {
                        logout();  // JEBYRNE: clear state on exception
                        e.printStackTrace();
                    }
                    //mview.redirectUserSetupActivity();  // JEBYRNE: this can only be called if COLLECTOR_ID_KEY is set
                }
            }

            @Override
            public void onError(Exception e) {
                if (mview == null) {
                    // JEBYRNE: crashalytics
                    // - https://console.firebase.google.com/u/1/project/collector-1581961981383/crashlytics/app/android:com.visym.collector/issues/a48b554829f928281e42fe269851018a?time=last-twenty-four-hours&sessionEventKey=617E8A96036B000115C4E46F46F6F450_1603651224978817432
                    // - It appears that mview can be null here
                    // - Best we can do is clear the user state
                    logout();  // JEBYRNE: clear state on exception
                }
                else if (e instanceof InvalidPasswordException) {
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

}
