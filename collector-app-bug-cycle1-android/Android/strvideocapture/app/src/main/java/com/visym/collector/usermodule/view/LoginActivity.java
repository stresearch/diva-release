package com.visym.collector.usermodule.view;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import com.amazonaws.amplify.generated.graphql.GetStrCollectorQuery;
import com.amazonaws.amplify.generated.graphql.SubjectByStrSubjectEmailQuery;
import com.amazonaws.amplify.generated.graphql.UpdateStrCollectorMutation;
import com.amazonaws.amplify.generated.graphql.UpdateStrSubjectMutation;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.visym.collector.R;
import com.visym.collector.dashboardmodule.view.DashboardActivity;
import com.visym.collector.network.ConnectivityReceiver;
import com.visym.collector.usermodule.IUserModule.ILoginPresenter;
import com.visym.collector.usermodule.IUserModule.ILoginView;
import com.visym.collector.usermodule.presenter.LoginActivityPresenter;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.EmailFieldTextWatcher;
import com.visym.collector.utils.EmptyFieldTextWatcher;
import com.visym.collector.utils.Globals;

import javax.annotation.Nonnull;

import butterknife.BindView;
import butterknife.ButterKnife;
import type.UpdateStrCollectorInput;
import type.UpdateStrSubjectInput;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener, ILoginView {


    @BindView(R.id.goBackBtn)
    ImageView goBackBtn;
    @BindView(R.id.userName)
    TextInputEditText userName;
    @BindView(R.id.userPassword)
    TextInputEditText userPassword;
    @BindView(R.id.userForgotPassword)
    AppCompatTextView userForgotPassword;
    @BindView(R.id.loginBtn)
    Button loginBtn;
    @BindView(R.id.signUptext)
    AppCompatTextView signUptext;
    @BindView(R.id.userNameLayout)
    TextInputLayout userNameLayout;
    @BindView(R.id.userPasswordLayout)
    TextInputLayout userPasswordLayout;
    private ILoginPresenter loginPresenter;
    private boolean loginRequestSent = false;
    private GraphQLCall.Callback<SubjectByStrSubjectEmailQuery.Data> subjectCallBack =
            new GraphQLCall.Callback<SubjectByStrSubjectEmailQuery.Data>() {
                @Override
                public void onResponse(@Nonnull Response<SubjectByStrSubjectEmailQuery.Data> response) {
                    runOnUiThread(() -> {
                        if (Globals.isShowingLoader()) {
                            Globals.dismissLoading();
                        }
                        if (!response.hasErrors() && response.data() != null) {
                            Intent actionIntent;
                            if (response.data().subjectByStrSubjectEmail().items().size() > 0) {
                                AppSharedPreference.getInstance().storeValue(Constant.IS_USER_CONSENTED, "true");
                                SubjectByStrSubjectEmailQuery.Item item =
                                        response.data().subjectByStrSubjectEmail().items().get(0);

                                updateSubjectTable(item.collector_email(), item.subject_email());
                                updateCollectorTable();

                                actionIntent = new Intent(LoginActivity.this,
                                        DashboardActivity.class);
                            } else {
                                actionIntent = new Intent(LoginActivity.this,
                                        DashboardActivity.class);
                            }
                            startActivity(actionIntent);
                            finish();
                        } else {
                            Intent redirectUserSetup = new Intent(LoginActivity.this, DashboardActivity.class);
                            startActivity(redirectUserSetup);
                            finish();
                        }
                    });
                }

                @Override
                public void onFailure(@Nonnull ApolloException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (Globals.isShowingLoader()) {
                                Globals.dismissLoading();
                            }
                            Intent redirectUserSetup = new Intent(LoginActivity.this, DashboardActivity.class);
                            startActivity(redirectUserSetup);
                            finish();
                        }
                    });
                }
            };
    private GraphQLCall.Callback<GetStrCollectorQuery.Data> getUserData = new GraphQLCall.Callback<GetStrCollectorQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<GetStrCollectorQuery.Data> response) {
            runOnUiThread(() -> {

                if (!response.hasErrors() && response.data() != null) {
                    GetStrCollectorQuery.GetStrCollector collectorDev = response.data().getStrCollector();
                    if (collectorDev != null) {
                        Boolean dropboxIntegrated = collectorDev.is_dropbox_integrated();
                        if (dropboxIntegrated != null) {
                            AppSharedPreference.getInstance().storeValue(Constant.IS_DROP_INTEGRATED, dropboxIntegrated);
                        }
                        Boolean paypalIntegrated = collectorDev.is_paypal_integrated();
                        if (paypalIntegrated != null) {
                            AppSharedPreference.getInstance().storeValue(Constant.IS_PAYPAL_INTEGRATED, paypalIntegrated);
                        }
                        updateConsentView(collectorDev.is_consented());
                    } else {
                        updateConsentView(false);
                    }
                } else {
                    updateConsentView(null);
                }
            });
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            runOnUiThread(() -> {
                if (Globals.isShowingLoader()) {
                    Globals.dismissLoading();
                }
                updateConsentView(null);
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        bindEvents();
        attachPresenter();

        String email = AppSharedPreference.getInstance().readString(Constant.COLLECTOR_EMAIL);
        if (!TextUtils.isEmpty(email)) {
            userName.setText(email);
            userPassword.requestFocus();
        }
    }

    private void attachPresenter() {
        if (loginPresenter == null) {
            loginPresenter = new LoginActivityPresenter();
        }
        loginPresenter.onViewAttached(this, this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.loginBtn:
                if (loginRequestSent) {
                    Toast.makeText(this, "You are logging in please wait",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                String username = userName.getText().toString();
                String password = userPassword.getText().toString();

                if (TextUtils.isEmpty(username)) {
                    userNameLayout.setError(getResources().getString(R.string.emptyEmail));
                    userPasswordLayout.setError(null);
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    userNameLayout.setError(null);
                    userPasswordLayout.setError(getResources().getString(R.string.emtpyPassword));
                    return;
                }

                if (!Globals.isValidEmail(username)) {
                    userNameLayout.setError(getResources().getString(R.string.invalid_email_message));
                    userPasswordLayout.setError(null);
                    return;
                }

                if (ConnectivityReceiver.isConnected()) {
                    Globals.showLoading(this);
                    loginRequestSent = true;
                    loginPresenter.loginApi(username, password);
                } else {
                    Globals.showSnackBar(getResources().getString(R.string.noInternet), this, Snackbar.LENGTH_SHORT);
                }
                break;

            case R.id.userForgotPassword:
                Intent forgotPwdIntent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(forgotPwdIntent);
                break;
            case R.id.goBackBtn:
                Intent frontScreenIntent = new Intent(LoginActivity.this, FrontScreenActivity.class);
                startActivity(frontScreenIntent);
                finish();
                break;
            case R.id.signUptext:
                Intent signInIntent = new Intent(LoginActivity.this, SignUpActivity.class);
                signInIntent.putExtra("from", "login");
                startActivity(signInIntent);
                finish();
                break;
        }
    }

    @Override
    public void bindEvents() {
        userForgotPassword.setOnClickListener(this);
        loginBtn.setOnClickListener(this);
        signUptext.setOnClickListener(this);
        goBackBtn.setOnClickListener(this);
        userName.addTextChangedListener(new EmailFieldTextWatcher(userNameLayout));
        userPassword.addTextChangedListener(new EmptyFieldTextWatcher(userPasswordLayout));
    }

    @Override
    public void redirectUserSetupActivity() {
        Globals.mAWSAppSyncClient.query(GetStrCollectorQuery.builder()
                .collector_email(AppSharedPreference.getInstance().readString(Constant.COLLECTOR_EMAIL))
                .collector_id(AppSharedPreference.getInstance().readString(Constant.COLLECTOR_ID_KEY)).build())
                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                .enqueue(getUserData);
    }

    private void updateConsentView(Boolean consented) {
        if (consented != null) {
            if (consented) {
                AppSharedPreference.getInstance().storeValue(Constant.IS_USER_CONSENTED, "true");
                Intent redirectUserSetup = new Intent(LoginActivity.this,
                        DashboardActivity.class);
                startActivity(redirectUserSetup);
                finish();
            } else {
                Globals.mAWSAppSyncClient.query(SubjectByStrSubjectEmailQuery.builder()
                        .subject_email(AppSharedPreference.getInstance().readString(Constant.COLLECTOR_EMAIL)).build())
                        .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                        .enqueue(subjectCallBack);
            }
        } else {
            Globals.mAWSAppSyncClient.query(SubjectByStrSubjectEmailQuery.builder()
                    .subject_email(AppSharedPreference.getInstance().readString(Constant.COLLECTOR_EMAIL)).build())
                    .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                    .enqueue(subjectCallBack);
        }
    }

    private void updateCollectorTable() {
        UpdateStrCollectorInput collectorInput = UpdateStrCollectorInput.builder()
                .collector_id(AppSharedPreference.getInstance().readString(Constant.COLLECTOR_ID_KEY))
                .collector_email(AppSharedPreference.getInstance().readString(Constant.COLLECTOR_EMAIL))
                .is_consented(true)
                .is_dropbox_integrated(false)
                .is_paypal_integrated(false)
                .build();
        Globals.getAppSyncClient().mutate(UpdateStrCollectorMutation.builder()
                .input(collectorInput).build());
    }

    private void updateSubjectTable(String collectorEmail, String subjectEmail) {
        UpdateStrSubjectInput subjectInput = UpdateStrSubjectInput.builder()
                .uuid(AppSharedPreference.getInstance().readString(Constant.COLLECTOR_ID_KEY))
                .collector_email(collectorEmail)
                .subject_email(subjectEmail)
                .build();
        Globals.getAppSyncClient().mutate(UpdateStrSubjectMutation.builder()
                .input(subjectInput).build());
    }

    @Override
    public void redirectEmailVerificationActivity(String email) {
        runOnUiThread(() -> {
            if (Globals.isShowingLoader()) {
                Globals.dismissLoading();
            }
            Intent emailIntent = new Intent(LoginActivity.this, EmailVerificationActivity.class);
            emailIntent.putExtra("email", email);
            startActivity(emailIntent);
            finish();
        });
    }

    @Override
    public void onRequestFailure(String errorMessage) {
        loginRequestSent = false;
        runOnUiThread(() -> {
            if (Globals.isShowingLoader()) {
                Globals.dismissLoading();
            }
            if (!TextUtils.isEmpty(errorMessage)) {
                Globals.showSnackBar(errorMessage, this, Snackbar.LENGTH_SHORT);
            } else {
                Globals.showSnackBar(getString(R.string.unknown_error_message), this, Snackbar.LENGTH_SHORT);
            }
        });
    }

    @Override
    public void redirectToResetPassword(String email) {
        runOnUiThread(() -> {
            if (Globals.isShowingLoader()) {
                Globals.dismissLoading();
            }
            Intent forgotPwdIntent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            forgotPwdIntent.putExtra("reset", true);
            forgotPwdIntent.putExtra("email", email);
            startActivity(forgotPwdIntent);
        });
    }

    @Override
    public void onDestroy() {
        if (Globals.isShowingLoader()) {
            Globals.dismissLoading();
        }
        loginPresenter.onViewDetached();
        super.onDestroy();
    }
}
