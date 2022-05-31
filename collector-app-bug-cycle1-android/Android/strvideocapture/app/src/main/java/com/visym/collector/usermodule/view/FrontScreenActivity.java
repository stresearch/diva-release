package com.visym.collector.usermodule.view;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.visym.collector.R;
import com.visym.collector.dashboardmodule.view.DashboardActivity;
import com.visym.collector.utils.Globals;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FrontScreenActivity extends AppCompatActivity implements View.OnClickListener {

    @BindView(R.id.signUpBtn)
    Button signUpBtn;
    @BindView(R.id.loginBtn)
    Button loginBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_front_screen);
        ButterKnife.bind(this);

        signUpBtn.setOnClickListener(this);
        loginBtn.setOnClickListener(this);

        boolean signedIn = AWSMobileClient.getInstance().isSignedIn();
        if (signedIn) {
            Intent redirectIntent = new Intent(FrontScreenActivity.this,
                    DashboardActivity.class);
            startActivity(redirectIntent);
            finish();
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.signUpBtn:
                Intent signIntent = new Intent(FrontScreenActivity.this, SignUpActivity.class);
                signIntent.putExtra("from", "front");
                startActivity(signIntent);
                finish();
                break;
            case R.id.loginBtn:
                Intent loginIntent = new Intent(FrontScreenActivity.this, LoginActivity.class);
                startActivity(loginIntent);
                finish();
                break;
        }
    }
}
