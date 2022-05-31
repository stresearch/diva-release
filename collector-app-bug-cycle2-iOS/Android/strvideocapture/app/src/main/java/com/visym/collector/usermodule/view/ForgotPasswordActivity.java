package com.visym.collector.usermodule.view;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.visym.collector.R;

public class ForgotPasswordActivity extends AppCompatActivity implements
        ForgotPasswordEmailFragment.FragmentAction, ForgotPasswordResetFragment.ResetFragmentAction {

    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        fragmentManager = getSupportFragmentManager();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("reset")) {
            String email = intent.getStringExtra("email");
            fragmentManager.beginTransaction().add(R.id.frame_container, ForgotPasswordResetFragment.getInstance(email))
                    .commit();
        } else {
            fragmentManager.beginTransaction().add(R.id.frame_container, ForgotPasswordEmailFragment.getInstance(this))
                    .addToBackStack(null).commit();
        }

    }

    @Override
    public void navigateToNextFragment(String emailAddress) {
        fragmentManager.beginTransaction().replace(R.id.frame_container,
                ForgotPasswordResetFragment.getInstance(emailAddress))
                .commit();
    }

    @Override
    public void onBackPressed() {
        int count = fragmentManager.getBackStackEntryCount();
        if (count > 1) {
            fragmentManager.popBackStack();
        } else {
            finish();
        }
    }
}
