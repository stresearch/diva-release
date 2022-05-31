package com.visym.collector.usermodule.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.Group;

import com.amazonaws.amplify.generated.graphql.GetStrCollectorQuery;
import com.amazonaws.amplify.generated.graphql.SubjectByStrSubjectEmailQuery;
import com.amazonaws.amplify.generated.graphql.UpdateStrCollectorMutation;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.dropbox.core.android.Auth;
import com.google.android.material.snackbar.Snackbar;
import com.paypal.android.sdk.payments.PayPalAuthorization;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalFuturePaymentActivity;
import com.paypal.android.sdk.payments.PayPalOAuthScopes;
import com.paypal.android.sdk.payments.PayPalProfileSharingActivity;
import com.paypal.android.sdk.payments.PayPalService;
import com.visym.collector.BuildConfig;
import com.visym.collector.R;
import com.visym.collector.capturemodule.views.EditConsentActivity;
import com.visym.collector.model.UserModel;
import com.visym.collector.network.ConnectivityReceiver;
import com.visym.collector.paypal.identitys.IdentityContract;
import com.visym.collector.paypal.identitys.IdentityDataModel;
import com.visym.collector.paypal.identitys.IdentityPresenter;
import com.visym.collector.paypal.tokens.AuthorizationCodeContract;
import com.visym.collector.paypal.tokens.AuthorizationCodeDataModel;
import com.visym.collector.paypal.tokens.AuthorizationCodePresenter;
import com.visym.collector.usermodule.IUserModule.IUserProfileView;
import com.visym.collector.usermodule.presenter.UserProfileActivityPresenter;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.Globals;
import com.visym.collector.utils.SharedPref;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import butterknife.BindView;
import butterknife.ButterKnife;
import type.UpdateStrCollectorInput;

public class UserProfileActivity extends AppCompatActivity implements View.OnClickListener, IUserProfileView, AuthorizationCodeContract.View, IdentityContract.View {

    private static final int ACCOUNT_EDIT_CODE = 1000;
    private PayPalConfiguration config;

    @BindView(R.id.closeBtn)
    ImageView closeBtn;
    @BindView(R.id.selectLinearLayout)
    LinearLayout selectLinearLayout;
    @BindView(R.id.accountBtn)
    TextView accountBtn;
    @BindView(R.id.statsBtn)
    TextView statsBtn;
    @BindView(R.id.userName)
    TextView userName;
    @BindView(R.id.userEmail)
    TextView userEmail;
    @BindView(R.id.editAccountBtn)
    TextView editAccountBtn;
    @BindView(R.id.paypalEmail)
    TextView paypalEmail;
    @BindView(R.id.paypalActionBtn)
    TextView paypalActionBtn;
    @BindView(R.id.connectDropBox)
    TextView connectDropBox;
    @BindView(R.id.syncSwitch)
    SwitchCompat syncSwitch;
    @BindView(R.id.helpAndSupport)
    TextView helpAndSupport;
    @BindView(R.id.signOutBtn)
    TextView signOutBtn;
    @BindView(R.id.revokeBtn)
    Button revokeBtn;
    @BindView(R.id.payment)
    TextView paymentText;
    @BindView(R.id.accountGroup)
    Group accountGroup;
    @BindView(R.id.statsGroup)
    Group statsGroup;
    @BindView(R.id.uploadedValue)
    TextView uploadedValue;
    @BindView(R.id.verifiedValue)
    TextView verifiedValue;
    @BindView(R.id.notVerifiedValue)
    TextView notVerifiedValue;
    @BindView(R.id.consentedValue)
    TextView consentedValue;
    @BindView(R.id.connectToPaypalGroup)
    Group connectToPaypalGroup;
    @BindView(R.id.connectToPaypalBtn)
    TextView connectToPaypalBtn;
    @BindView(R.id.amountGroup)
    Group amountGroup;
    @BindView(R.id.amountRecievedValue)
    TextView amountRecievedValue;
    @BindView(R.id.outStandingAmountValue)
    TextView outStandingAmountValue;
    @BindView(R.id.editConsent)
    TextView editConsent;
    @BindView(R.id.dropBoxOption)
    TextView dropBoxOption;
    private UserProfileActivityPresenter UPAPresenter;
    private AppSharedPreference preference;
    private Context mContext;
    private String uploadedCount, verifiedCount, notVerifiedCount, consentedCount, amountPaid, outstandingAmount;
    private GraphQLCall.Callback<SubjectByStrSubjectEmailQuery.Data> checkUserConsent = new GraphQLCall.Callback<SubjectByStrSubjectEmailQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<SubjectByStrSubjectEmailQuery.Data> response) {
            if (response != null) {
                if (response.data().subjectByStrSubjectEmail() != null) {
                    if (Globals.isShowingLoader()) {
                        Globals.dismissLoading();
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (preference.readString(Constant.IS_USER_CONSENTED) != null) {
                                editConsent.setVisibility(View.VISIBLE);
                                revokeBtn.setVisibility(View.GONE);
                            }
                            String email = preference.readString(Constant.COLLECTOR_EMAIL);
                            String firstname_lastname = preference.readString(Constant.COLLECTOR_FNAME) + " " + preference.readString(Constant.COLLECTOR_LNAME);

                            userName.setText(firstname_lastname);

                            if (!TextUtils.isEmpty(email)) {
                                userEmail.setText(email);
                            } else {
                                userEmail.setText("");
                            }
                        }
                    });
                    if (response.data().subjectByStrSubjectEmail().items().size() > 0) {
                        preference.storeValue(Constant.IS_USER_CONSENTED, "true");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (preference.readString(Constant.IS_USER_CONSENTED) != null) {
                                    editConsent.setVisibility(View.VISIBLE);
                                    revokeBtn.setVisibility(View.GONE);
                                }
                                String email = preference.readString(Constant.COLLECTOR_EMAIL);
                                String firstname_lastname = preference.readString(Constant.COLLECTOR_FNAME) + " " + preference.readString(Constant.COLLECTOR_LNAME);

                                userName.setText(firstname_lastname);

                                if (!TextUtils.isEmpty(email)) {
                                    userEmail.setText(email);
                                } else {
                                    userEmail.setText("");
                                }
                            }
                        });


                    }
                }
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
                    Globals.showSnackBar(getResources().getString(R.string.unknown_error_message), mContext, Snackbar.LENGTH_LONG);
                }
            });
        }
    };
    private GraphQLCall.Callback<UpdateStrCollectorMutation.Data> updateCollector = new GraphQLCall.Callback<UpdateStrCollectorMutation.Data>() {
        @Override
        public void onResponse(@Nonnull Response<UpdateStrCollectorMutation.Data> response) {
            if (Globals.isShowingLoader()) {
                Globals.dismissLoading();
            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    };
    private GraphQLCall.Callback<GetStrCollectorQuery.Data> getUserData = new GraphQLCall.Callback<GetStrCollectorQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<GetStrCollectorQuery.Data> response) {
            runOnUiThread(() -> {

                if (!response.hasErrors() && response.data() != null) {
                    if (Globals.isShowingLoader()) {
                        Globals.dismissLoading();
                    }
                    GetStrCollectorQuery.GetStrCollector collectorDev = response.data().getStrCollector();
                    if (collectorDev != null) {
                        Boolean payPalIntegrated = collectorDev.is_paypal_integrated();
                        if (payPalIntegrated != null && payPalIntegrated) {
                            paypalActionBtn.setText("Change");
                            String id = collectorDev.paypal_email_id();
                            if (id != null && !id.equals("0")){
                                preference.storeValue(Constant.PAYPAL_EMAIL, id);
                                paypalEmail.setText(id);
                            }
                        } else {
                            paypalActionBtn.setText("Connect");
                            paypalEmail.setText("Paypal");
                        }
                    } else {
                        paypalActionBtn.setText("Connect");
                        paypalEmail.setText("Paypal");

                    }
                    if (collectorDev != null && collectorDev.dropbox_token() != null) {
                        if (!collectorDev.dropbox_token().contentEquals("0")) {
                            preference.storeValue(Constant.DROPBOX_ACCESSTOKEN, collectorDev.dropbox_token());
                            connectDropBox.setText("Remove");
                        } else {
                            connectDropBox.setText("Connect");
                        }
                    } else {
                        connectDropBox.setText("Connect");
                    }
                    if (response.data().getStrCollector().authorized() != null) {
                        amountPaid = response.data().getStrCollector().authorized();
                    } else {
                        amountPaid = "0";
                    }
                    if (response.data().getStrCollector().uploaded_count() != null) {
                        uploadedCount = response.data().getStrCollector().uploaded_count();
                    } else {
                        uploadedCount = "0";
                    }
                    if (response.data().getStrCollector().verified_count() != null) {
                        verifiedCount = response.data().getStrCollector().verified_count();
                    } else {
                        verifiedCount = "0";
                    }
                    if (response.data().getStrCollector().not_verified_count() != null) {
                        notVerifiedCount = response.data().getStrCollector().not_verified_count();
                    } else {
                        notVerifiedCount = "0";
                    }
                    if (response.data().getStrCollector().consented_count() != null) {
                        consentedCount = response.data().getStrCollector().consented_count();
                    } else {
                        consentedCount = "0";
                    }
                    if (response.data().getStrCollector().outstanding_amount() != null) {
                        outstandingAmount = response.data().getStrCollector().outstanding_amount();
                    } else {
                        outstandingAmount = "0";
                    }
                }
            });

        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            runOnUiThread(() -> {
                if (Globals.isShowingLoader()) {
                    Globals.dismissLoading();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        ButterKnife.bind(this);
        preference = AppSharedPreference.getInstance();
        attachPresenter();
        config = new PayPalConfiguration()
                // Start with mock environment.  When ready, switch to sandbox (ENVIRONMENT_SANDBOX)
                // or live (ENVIRONMENT_PRODUCTION)
                //.environment(PayPalConfiguration.ENVIRONMENT_NO_NETWORK)
                .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)
                .clientId(com.visym.collector.paypal.PayPalConfiguration.PAYPAL_CLIENT_ID)
                // Minimally, you will need to set three merchant information properties.
                // These should be the same values that you provided to PayPal when you registered your app.
                .merchantName(getString(R.string.app_name))
                .merchantPrivacyPolicyUri(Uri.parse("https://www.example.com/privacy"))
                .merchantUserAgreementUri(Uri.parse("https://www.example.com/legal"));

        initUserProfile();
        bindEvents();
        mContext = this;
        if (preference.readString(Constant.DROPBOX_ACCESSTOKEN) != null) {
            connectDropBox.setText("Remove");
        } else {
            connectDropBox.setText("Connect");
        }
        if (preference.readString(Constant.PAYPAL_EMAIL) != null) {
            paypalActionBtn.setText("Change");
        } else {
            paypalActionBtn.setText("Connect");
        }
        paypalActionBtn.setOnClickListener(this);
        connectDropBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Auth.startOAuth2Authentication(UserProfileActivity.this, getString(R.string.APP_KEY));

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

    private void initUserProfile() {
        if (preference.readString(Constant.IS_USER_CONSENTED) != null) {
            editConsent.setVisibility(View.VISIBLE);
            revokeBtn.setVisibility(View.GONE);
            String email = preference.readString(Constant.COLLECTOR_EMAIL);
            String firstname_lastname = preference.readString(Constant.COLLECTOR_FNAME) + " " + preference.readString(Constant.COLLECTOR_LNAME);

            userName.setText(firstname_lastname);

            if (!TextUtils.isEmpty(email)) {
                userEmail.setText(email);
            } else {
                userEmail.setText("");
            }
        } else {
            Globals.showLoading(this);
            Globals.getAppSyncClient().query(SubjectByStrSubjectEmailQuery.builder()
                    .subject_email(preference.readString(Constant.COLLECTOR_EMAIL))
                    .build())
                    .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                    .enqueue(checkUserConsent);
        }
    }

    public void attachPresenter() {
        if (UPAPresenter == null) {
            UPAPresenter = new UserProfileActivityPresenter();
        }
        UPAPresenter.onViewAttached(this, this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.accountBtn:
                displayAccountInformation();
                break;
            case R.id.statsBtn:
                displayStatsInformation();

                break;
            case R.id.editAccountBtn:
                Intent editAccount = new Intent(this, UserProfileEditActivity.class);
                startActivityForResult(editAccount, ACCOUNT_EDIT_CODE);
                break;
            case R.id.paypalActionBtn:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(UserProfileActivity.this, PayPalService.class);
                        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
                        startService(intent);
                        onProfileSharingPressed();
                    }
                });
                break;
            case R.id.connectDropBox:
                Auth.startOAuth2Authentication(UserProfileActivity.this, getString(R.string.APP_KEY));
                break;
            case R.id.signOutBtn:
                if (ConnectivityReceiver.isConnected()) {
                    Globals.showLoading(this);
                    UPAPresenter.signOutFunctionality();
                } else {
                    Globals.showSnackBar(getResources().getString(R.string.noInternet), this,
                            Snackbar.LENGTH_SHORT);
                }
                break;
            case R.id.revokeBtn:
                UPAPresenter.revokeUserConsentForTheDay();
                break;
            case R.id.editConsent:
                Intent editConsent = new Intent(UserProfileActivity.this, EditConsentActivity.class);
                startActivity(editConsent);
                break;
            case R.id.closeBtn:
                finish();
        }
    }

    @Override
    public void displayAccountInformation() {
        Globals.showLoading(this);
        accountBtn.setBackgroundColor(getResources().getColor(R.color.goldColor));
        statsBtn.setBackgroundColor(getResources().getColor(R.color.textLightColor));
        statsBtn.setBackground(getResources().getDrawable(R.drawable.custom_gold_border));
        accountBtn.setTextColor(getResources().getColor(R.color.textLightColor));
        statsBtn.setTextColor(getResources().getColor(R.color.darkBlack));
        statsGroup.setVisibility(View.GONE);
        connectToPaypalBtn.setOnClickListener(this);
        accountGroup.setVisibility(View.VISIBLE);
        revokeBtn.setVisibility(View.VISIBLE);
        editConsent.setVisibility(View.VISIBLE);
        amountGroup.setVisibility(View.GONE);
        connectDropBox.setVisibility(View.VISIBLE);
        connectToPaypalBtn.setVisibility(View.VISIBLE);
        dropBoxOption.setVisibility(View.VISIBLE);
        paypalActionBtn.setVisibility(View.VISIBLE);
        paypalEmail.setVisibility(View.VISIBLE);
        paymentText.setVisibility(View.VISIBLE);
        // connectToPaypalGroup.setVisibility(View.VISIBLE);
    /*try {
      JSONObject accountInformation = new JSONObject(userModel.getAccount());
      userEmail.setText(accountInformation.get("email").toString());
      paypalEmail.setText(accountInformation.get("paypalUserName").toString());
      if (accountInformation.getBoolean("syncOverWifi")) {
        syncSwitch.setChecked(true);
        sharedPref
            .setBooleanToSharedPref("syncOverWifi", accountInformation.getBoolean("syncOverWifi"));
      } else {
        syncSwitch.setChecked(false);
        sharedPref
            .setBooleanToSharedPref("syncOverWifi", accountInformation.getBoolean("syncOverWifi"));
      }
      Globals.dismissLoading();

    } catch (JSONException e) {
      e.printStackTrace();
    }*/

        if (preference.readString(Constant.IS_USER_CONSENTED) != null) {
            editConsent.setVisibility(View.VISIBLE);
            revokeBtn.setVisibility(View.GONE);
        }
        String email = preference.readString(Constant.COLLECTOR_EMAIL);
        String firstname_lastname = preference.readString(Constant.COLLECTOR_FNAME) + " " + preference.readString(Constant.COLLECTOR_LNAME);

        userName.setText(firstname_lastname);

        if (!TextUtils.isEmpty(email)) {
            userEmail.setText(email);
        } else {
            userEmail.setText("");
        }
        Globals.dismissLoading();
    }

    @Override
    public void displayStatsInformation() {
        Globals.showLoading(this);
        selectLinearLayout
                .setBackground(this.getResources().getDrawable(R.drawable.custom_gold_border));
        accountBtn.setBackgroundColor(getResources().getColor(R.color.textLightColor));
        accountBtn.setBackground(getResources().getDrawable(R.drawable.custom_gold_border));
        statsBtn.setBackgroundColor(getResources().getColor(R.color.goldColor));
        accountBtn.setTextColor(getResources().getColor(R.color.darkBlack));
        statsBtn.setTextColor(getResources().getColor(R.color.textLightColor));
        accountGroup.setVisibility(View.GONE);
        editConsent.setVisibility(View.GONE);
        revokeBtn.setVisibility(View.GONE);
        connectToPaypalGroup.setVisibility(View.GONE);
        connectDropBox.setVisibility(View.GONE);
        connectToPaypalBtn.setVisibility(View.GONE);
        dropBoxOption.setVisibility(View.GONE);
        paypalActionBtn.setVisibility(View.GONE);
        paypalEmail.setVisibility(View.GONE);
        paymentText.setVisibility(View.GONE);
        amountGroup.setVisibility(View.VISIBLE);
        statsGroup.setVisibility(View.VISIBLE);
        String amountRecieved = "$" + amountPaid;
        String outstandingAmountDollars = "$" + outstandingAmount;

        uploadedValue.setText(uploadedCount);
        verifiedValue.setText(verifiedCount);
        notVerifiedValue.setText(notVerifiedCount);
        consentedValue.setText(consentedCount);
        amountRecievedValue.setText(amountRecieved);
        outStandingAmountValue.setText(outstandingAmountDollars);
        Globals.dismissLoading();
    }

    @Override
    public void redirectToTheFrontScreen() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Globals.isShowingLoader()) {
                    Globals.dismissLoading();
                }
                Intent signoutIntent = new Intent(UserProfileActivity.this, LoginActivity.class);
                signoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(signoutIntent);
            }
        });
    }

    @Override
    public void bindEvents() {
        accountBtn.setOnClickListener(this);
        statsBtn.setOnClickListener(this);
        editAccountBtn.setOnClickListener(this);
        paypalActionBtn.setOnClickListener(this);
        connectDropBox.setOnClickListener(this);
        helpAndSupport.setOnClickListener(this);
        signOutBtn.setOnClickListener(this);
        revokeBtn.setOnClickListener(this);
        closeBtn.setOnClickListener(this);
        editConsent.setOnClickListener(this);
        syncSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.e(Globals.TAG, "onCheckedChanged: switchedOn");
                }

                Log.e(Globals.TAG, "onCheckedChanged: switch off");
            }
        });
    }

    @Override
    public void onLogoutFailure(String errorMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Globals.isShowingLoader()) {
                    Globals.dismissLoading();
                }
                Globals.showSnackBar(errorMessage, UserProfileActivity.this, Snackbar.LENGTH_SHORT);
            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UPAPresenter.onViewDetached();
    }

    @Override
    public void onResume() {
        super.onResume();
        getAccessToken();
        if (UPAPresenter == null) {
            attachPresenter();
        }
        preference.readString(Constant.COLLECTOR_FNAME);
        preference.readString(Constant.COLLECTOR_LNAME);

    }

    @Override
    public void refreshActivity() {
        startActivity(getIntent());
    }

    public void getAccessToken() {
        String accessToken = Auth.getOAuth2Token();
        if (accessToken != null) {
            preference.storeValue(Constant.DROPBOX_ACCESSTOKEN, accessToken);
            Globals.showLoading(this);

            UpdateStrCollectorInput collectorInput = UpdateStrCollectorInput.builder()
                    .collector_email(AppSharedPreference.getInstance().readString(Constant.COLLECTOR_EMAIL))
                    .collector_id(AppSharedPreference.getInstance().readString(Constant.COLLECTOR_ID_KEY))
                    .dropbox_token(AppSharedPreference.getInstance().readString(Constant.DROPBOX_ACCESSTOKEN))
                    .is_dropbox_integrated(true)
                    .build();
            Globals.mAWSAppSyncClient.mutate(UpdateStrCollectorMutation.builder().input(collectorInput).build())
                    .enqueue(updateCollector);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectDropBox.setText("Change");
                }
            });

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACCOUNT_EDIT_CODE && resultCode == Activity.RESULT_OK) {
            String firstname_lastname = preference.readString(Constant.COLLECTOR_FNAME)
                    + " " + preference.readString(Constant.COLLECTOR_LNAME);
            userName.setText(firstname_lastname);
        } else if (resultCode == Activity.RESULT_OK) {
            PayPalAuthorization auth = data.getParcelableExtra(PayPalProfileSharingActivity.EXTRA_RESULT_AUTHORIZATION);
            if (auth != null) {
                String authorization_code = auth.getAuthorizationCode();
                AuthorizationCodePresenter authorizationCodePresenter = new AuthorizationCodePresenter(UserProfileActivity.this);
                authorizationCodePresenter.requestTokenData(authorization_code, "authorization_code", "code", UserProfileActivity.this);
             }
        } else if (resultCode == Activity.RESULT_CANCELED) {
          Toast.makeText(mContext, "The user canceled.", Toast.LENGTH_SHORT).show();
        } else if (resultCode == PayPalFuturePaymentActivity.RESULT_EXTRAS_INVALID) {
          Toast.makeText(mContext, "Probably the attempt to previously start the PayPalService had an invalid PayPalConfiguration. Please see the docs.", Toast.LENGTH_SHORT).show();
        }
    }

    public void onProfileSharingPressed() {
        Intent intent = new Intent(UserProfileActivity.this, PayPalProfileSharingActivity.class);
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
    public void showIdentityProgress() {  }

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
                    paypalActionBtn.setText("Change");
                    paypalEmail.setText(data.getEmails().get(0).getValue());

                }
            });
        } else {
            Toast.makeText(UserProfileActivity.this, data.getMessage(), Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onResponseIdentityFailure(Throwable throwable, IdentityDataModel data) {
      Toast.makeText(mContext, data.getMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showTokenProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Globals.showLoading(UserProfileActivity.this);
            }
        });
    }

    @Override
    public void hideTokenProgress() {  }

    @Override
    public void onResponseTokenSuccess(AuthorizationCodeDataModel data) {
      if (data.getApiStatus().equalsIgnoreCase("success")) {
            if (data.getRefresh_token() != null) {
                AuthorizationCodePresenter authorizationCodePresenter = new AuthorizationCodePresenter(UserProfileActivity.this);
                authorizationCodePresenter.requestTokenData(data.getRefresh_token(), "refresh_token", "refresh_token", UserProfileActivity.this);
            } else {
                IdentityPresenter identityPresenter = new IdentityPresenter(UserProfileActivity.this);
                identityPresenter.requestIdentityData(data.getAccess_token(), UserProfileActivity.this);
            }
        } else {
            Toast.makeText(UserProfileActivity.this, data.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResponseTokenFailure(Throwable throwable, AuthorizationCodeDataModel data) {
            Toast.makeText(UserProfileActivity.this,data.getMessage(),Toast.LENGTH_SHORT).show();
    }
}
