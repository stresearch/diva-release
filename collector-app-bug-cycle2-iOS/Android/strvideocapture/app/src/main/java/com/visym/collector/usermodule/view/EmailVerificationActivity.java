package com.visym.collector.usermodule.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.results.SignUpResult;
import com.amazonaws.services.cognitoidentityprovider.model.InvalidParameterException;
import com.visym.collector.R;
import com.visym.collector.network.ApiClient;
import com.visym.collector.network.ApiInterface;
import com.visym.collector.network.NetworkRequest;
import com.visym.collector.utils.Globals;

import butterknife.BindView;
import butterknife.ButterKnife;


public class EmailVerificationActivity extends AppCompatActivity implements View.OnClickListener {

    @BindView(R.id.closeBtn)
    ImageView closeBtn;
    @BindView(R.id.userEmail)
    TextView userEmail;
    @BindView(R.id.userResendEmailVerification)
    TextView resendEmail;
    String email;
    ApiInterface api;
    NetworkRequest networkRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);
        ButterKnife.bind(this);
        api = ApiClient.buildService();
        networkRequest = NetworkRequest.getInstance();
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("email")) {
            email = getIntent().getStringExtra("email");
            userEmail.setText(email);
        }
        userEmail.setText(email);
        closeBtn.setOnClickListener(this);
        resendEmail.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.closeBtn:
                onBackPressed();
                break;
            case R.id.userResendEmailVerification:
                if (TextUtils.isEmpty(email)) {
                    userEmail.setText("Email ID not found");
                    return;
                }
                String emailHint = resendEmail.getText().toString();
                if (!TextUtils.isEmpty(emailHint) && emailHint.contentEquals("Please check your email")) {
                    return;
                }
                Globals.showLoading(EmailVerificationActivity.this);
                AWSMobileClient.getInstance().resendSignUp(email, new Callback<SignUpResult>() {
                    @Override
                    public void onResult(SignUpResult result) {
                        runOnUiThread(() -> {
                            Globals.dismissLoading();
                            resendEmail.setText("Please check your email");
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> {
                            Globals.dismissLoading();
                            if (e instanceof InvalidParameterException) {
                                Toast.makeText(EmailVerificationActivity.this,
                                        ((InvalidParameterException) e).getErrorMessage(), Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(EmailVerificationActivity.this, getString(R.string.unknown_error_message),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
        }
    }

    @Override
    public void onBackPressed() {
        Intent redirectIntent = new Intent(EmailVerificationActivity.this, LoginActivity.class);
        startActivity(redirectIntent);
        finish();
    }
}
