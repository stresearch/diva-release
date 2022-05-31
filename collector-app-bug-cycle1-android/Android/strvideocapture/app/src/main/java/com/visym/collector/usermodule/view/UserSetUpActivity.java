package com.visym.collector.usermodule.view;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import com.amazonaws.amplify.generated.graphql.GetStrCollectorQuery;
import com.amazonaws.amplify.generated.graphql.SubjectByStrSubjectEmailQuery;
import com.amazonaws.amplify.generated.graphql.UpdateStrCollectorMutation;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.dropbox.core.android.Auth;
import com.paypal.android.sdk.payments.PayPalAuthorization;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalFuturePaymentActivity;
import com.paypal.android.sdk.payments.PayPalOAuthScopes;
import com.paypal.android.sdk.payments.PayPalProfileSharingActivity;
import com.paypal.android.sdk.payments.PayPalService;
import com.visym.collector.R;
import com.visym.collector.capturemodule.views.ConsentConfirmationActivity;
import com.visym.collector.dashboardmodule.view.DashboardActivity;
import com.visym.collector.paypal.identitys.IdentityContract;
import com.visym.collector.paypal.identitys.IdentityDataModel;
import com.visym.collector.paypal.identitys.IdentityPresenter;
import com.visym.collector.paypal.tokens.AuthorizationCodeContract;
import com.visym.collector.paypal.tokens.AuthorizationCodeDataModel;
import com.visym.collector.paypal.tokens.AuthorizationCodePresenter;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.Globals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import butterknife.BindView;
import butterknife.ButterKnife;
import type.UpdateStrCollectorInput;

public class UserSetUpActivity extends AppCompatActivity implements View.OnClickListener, AuthorizationCodeContract.View, IdentityContract.View {
    private static PayPalConfiguration config = new PayPalConfiguration()

            // Start with mock environment.  When ready, switch to sandbox (ENVIRONMENT_SANDBOX)
            // or live (ENVIRONMENT_PRODUCTION)
            //.environment(PayPalConfiguration.ENVIRONMENT_NO_NETWORK)
            .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)

            .clientId(com.visym.collector.paypal.PayPalConfiguration.PAYPAL_CLIENT_ID)

            // Minimally, you will need to set three merchant information properties.
            // These should be the same values that you provided to PayPal when you registered your app.
            .merchantName("Example Store")
            .merchantPrivacyPolicyUri(Uri.parse("https://www.example.com/privacy"))
            .merchantUserAgreementUri(Uri.parse("https://www.example.com/legal"));
    @BindView(R.id.saveBtn)
    Button saveBtn;
    @BindView(R.id.skipText)
    AppCompatTextView skipText;
    @BindView(R.id.consentRecord)
    TextView consentRecord;
    @BindView(R.id.consentStatus)
    TextView consentStatus;
    @BindView(R.id.dropboxconnect)
    TextView dropboxconnect;
    @BindView(R.id.paypalconnect)
    TextView paypalconnect;
    @BindView(R.id.paypalEmail)
    TextView paypalEmail;
    String accessToken;
    private AppSharedPreference preference;
    private GraphQLCall.Callback<UpdateStrCollectorMutation.Data> updateCollector = new GraphQLCall.Callback<UpdateStrCollectorMutation.Data>() {
        @Override
        public void onResponse(@Nonnull Response<UpdateStrCollectorMutation.Data> response) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Globals.isShowingLoader()) {
                        Globals.dismissLoading();
                    }
                }
            });
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(UserSetUpActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    };
    private GraphQLCall.Callback<SubjectByStrSubjectEmailQuery.Data> getCollectorFromSubjectResponse = new GraphQLCall.Callback<SubjectByStrSubjectEmailQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<SubjectByStrSubjectEmailQuery.Data> response) {
            if (response != null && response.data() != null && response.data().subjectByStrSubjectEmail() != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        consentRecord.setVisibility(View.GONE);
                        consentStatus.setText("Consent Given");
                        if (Globals.isShowingLoader()) {
                            Globals.dismissLoading();
                        }
                        UpdateStrCollectorInput collectorInput = UpdateStrCollectorInput.builder()
                                .collector_email(AppSharedPreference.getInstance().readString(Constant.COLLECTOR_EMAIL))
                                .collector_id(AppSharedPreference.getInstance().readString(Constant.COLLECTOR_ID_KEY))
                                .is_consented(true)
                                .build();
                        Globals.mAWSAppSyncClient.mutate(UpdateStrCollectorMutation.builder().input(collectorInput).build())
                                .enqueue(updateCollector);
                    }
                });

            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Globals.isShowingLoader()) {
                            Globals.dismissLoading();
                        }
                        consentRecord.setVisibility(View.VISIBLE);
                        consentStatus.setText("Consent Pending");
                    }
                });
            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Globals.isShowingLoader()) {
                        Globals.dismissLoading();
                    }
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
                        if ((collectorDev.dropbox_token() != null) && (collectorDev.dropbox_token().length() > 0)) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    paypalconnect.setText("Change");
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    paypalconnect.setText("Connect");
                                }
                            });
                        }
                    } else {
                        paypalconnect.setText("Connect");
                    }
                    if (collectorDev != null) {
                        if ((collectorDev.dropbox_token() != null) && (collectorDev.dropbox_token().length() > 0)) {
                            preference.storeValue(Constant.DROPBOX_ACCESSTOKEN, collectorDev.dropbox_token());
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dropboxconnect.setText("Remove");
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dropboxconnect.setText("Connect");
                                }
                            });
                        }
                    } else {
                        dropboxconnect.setText("Connect");
                    }
                    if (collectorDev != null) {
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
        setContentView(R.layout.activity_user_set_up);
        ButterKnife.bind(this);
        saveBtn.setOnClickListener(this);
        skipText.setOnClickListener(this);
        consentRecord.setOnClickListener(this);
        preference = AppSharedPreference.getInstance();
        if ((preference.readString(Constant.DROPBOX_ACCESSTOKEN) != null) && (preference.readString(Constant.DROPBOX_ACCESSTOKEN).length() > 0)) {
            dropboxconnect.setText("Change");
        } else {
            dropboxconnect.setText("Connect");
        }
        if (preference.readString(Constant.PAYPAL_EMAIL) != null) {
            paypalconnect.setText("Change");
        } else {
            paypalconnect.setText("Connect");
        }

        paypalconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(UserSetUpActivity.this, PayPalService.class);
                        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
                        startService(intent);
                        onProfileSharingPressed();
                    }
                });

            }
        });

        dropboxconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Auth.startOAuth2Authentication(UserSetUpActivity.this, getString(R.string.APP_KEY));

                    }
                });

            }
        });
        Globals.showLoading(this);
        Globals.mAWSAppSyncClient.query(GetStrCollectorQuery.builder()
                .collector_email(AppSharedPreference.getInstance().readString(Constant.COLLECTOR_EMAIL))
                .collector_id(AppSharedPreference.getInstance().readString(Constant.COLLECTOR_ID_KEY)).build())
                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                .enqueue(getUserData);


    }

    private void updateConsentView(Boolean consented) {
        if (consented != null) {
            if (consented) {
                if (Globals.isShowingLoader()) {
                    Globals.dismissLoading();
                }
                consentRecord.setVisibility(View.GONE);
                consentStatus.setText("Consent Given");
            } else {
                Globals.mAWSAppSyncClient.query(SubjectByStrSubjectEmailQuery.builder()
                        .subject_email(AppSharedPreference.getInstance().readString(Constant.COLLECTOR_EMAIL)).build())
                        .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                        .enqueue(getCollectorFromSubjectResponse);
            }
        } else {
            if (Globals.isShowingLoader()) {
                Globals.dismissLoading();
            }
            consentRecord.setVisibility(View.VISIBLE);
            consentStatus.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.saveBtn:
            case R.id.skipText:
                Intent redirectIntent = new Intent(UserSetUpActivity.this, DashboardActivity.class);
                redirectIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(redirectIntent);
                finish();
                break;
            case R.id.consentRecord:
                Intent consentIntent = new Intent(UserSetUpActivity.this, ConsentConfirmationActivity.class);
                Globals.isSetupFlow = true;
                startActivity(consentIntent);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    public void onDestroy() {
        stopService(new Intent(this, PayPalService.class));
        Globals.dismissLoading();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            PayPalAuthorization auth = data.getParcelableExtra(PayPalProfileSharingActivity.EXTRA_RESULT_AUTHORIZATION);
            if (auth != null) {
                String authorization_code = auth.getAuthorizationCode();
                AuthorizationCodePresenter authorizationCodePresenter = new AuthorizationCodePresenter(UserSetUpActivity.this);
                authorizationCodePresenter.requestTokenData(authorization_code, "authorization_code", "code", UserSetUpActivity.this);
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, "The user canceled", Toast.LENGTH_SHORT).show();
        } else if (resultCode == PayPalFuturePaymentActivity.RESULT_EXTRAS_INVALID) {
            Toast.makeText(this, "Probably the attempt to previously start the PayPalService had an invalid PayPalConfiguration. Please see the docs.", Toast.LENGTH_SHORT).show();
        }
    }

    public void onProfileSharingPressed() {
        Intent intent = new Intent(UserSetUpActivity.this, PayPalProfileSharingActivity.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        intent.putExtra(PayPalProfileSharingActivity.EXTRA_REQUESTED_SCOPES, getOauthScopes());
        startActivityForResult(intent, 2);
    }

    private PayPalOAuthScopes getOauthScopes() {
        Set<String> scopes = new HashSet<String>(
                Arrays.asList(PayPalOAuthScopes.PAYPAL_SCOPE_EMAIL));
        return new PayPalOAuthScopes(scopes);
    }

    @Override
    public void showTokenProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Globals.showLoading(UserSetUpActivity.this);
            }
        });
    }

    @Override
    public void hideTokenProgress() {

    }

    @Override
    public void onResponseTokenSuccess(AuthorizationCodeDataModel data) {
        if (data.getApiStatus().equalsIgnoreCase("success")) {
            if (data.getRefresh_token() != null) {
                AuthorizationCodePresenter authorizationCodePresenter = new AuthorizationCodePresenter(UserSetUpActivity.this);
                authorizationCodePresenter.requestTokenData(data.getRefresh_token(), "refresh_token", "refresh_token", UserSetUpActivity.this);
            } else {
                IdentityPresenter identityPresenter = new IdentityPresenter(UserSetUpActivity.this);
                identityPresenter.requestIdentityData(data.getAccess_token(), UserSetUpActivity.this);
            }
        } else {
            Toast.makeText(UserSetUpActivity.this, data.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResponseTokenFailure(Throwable throwable, AuthorizationCodeDataModel data) {
    }

    @Override
    public void showIdentityProgress() {
    }

    @Override
    public void hideIdentityProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Globals.isShowingLoader()) {
                    Globals.dismissLoading();
                }
            }
        });

    }

    @Override
    public void onResponseIdentitySuccess(IdentityDataModel data) {

        if (data.getApiStatus().equalsIgnoreCase("success")) {
            Globals.showLoading(this);
            preference.storeValue(Constant.PAYPAL_EMAIL, data.getEmails().get(0).getValue());
            UpdateStrCollectorInput collectorInput = UpdateStrCollectorInput.builder()
                    .collector_email(AppSharedPreference.getInstance().readString(Constant.COLLECTOR_EMAIL))
                    .collector_id(AppSharedPreference.getInstance().readString(Constant.COLLECTOR_ID_KEY))
                    .paypal_email_id(AppSharedPreference.getInstance().readString(Constant.PAYPAL_EMAIL))
                    .is_paypal_integrated(true)
                    .build();
            Globals.mAWSAppSyncClient.mutate(UpdateStrCollectorMutation.builder().input(collectorInput).build())
                    .enqueue(updateCollector);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    paypalconnect.setText("Change");
                    paypalEmail.setText(data.getEmails().get(0).getValue());
                }
            });
        } else {
            Toast.makeText(UserSetUpActivity.this, data.getMessage(), Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onResponseIdentityFailure(Throwable throwable, IdentityDataModel data) {
        Toast.makeText(this, data.getMessage(), Toast.LENGTH_SHORT).show();
    }
}
