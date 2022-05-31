package com.visym.collector.usermodule.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.visym.collector.R;
import com.visym.collector.usermodule.IUserModule.ISignUpView;
import com.visym.collector.usermodule.presenter.SignUpActivityPresenter;
import com.visym.collector.utils.EmailFieldTextWatcher;
import com.visym.collector.utils.EmptyFieldTextWatcher;
import com.visym.collector.utils.FieldValidator;
import com.visym.collector.utils.Globals;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener, ISignUpView {

    @BindView(R.id.userFirstName)
    TextInputEditText userFirstName;
    @BindView(R.id.userFirstNameLayout)
    TextInputLayout userFirstNameLayout;
    @BindView(R.id.userLastName)
    TextInputEditText userLastName;
    @BindView(R.id.userLastNameLayout)
    TextInputLayout userLastNameLayout;
    @BindView(R.id.userEmail)
    TextInputEditText userEmail;
    @BindView(R.id.userEmailLayout)
    TextInputLayout userEmailLayout;
    @BindView(R.id.userPassword)
    TextInputEditText userPassword;
    @BindView(R.id.userPasswordLayout)
    TextInputLayout userPasswordLayout;
    @BindView(R.id.userConfPassword)
    TextInputEditText userConfPassword;
    @BindView(R.id.userConfPasswordLayout)
    TextInputLayout userConfPasswordLayout;
    @BindView(R.id.signUpBtn)
    Button signUpBtn;
    @BindView(R.id.termsText)
    AppCompatTextView termsText;
    @BindView(R.id.loginText)
    AppCompatTextView loginText;
    @BindView(R.id.goBackBtn)
    ImageView goBackBtn;
    SignUpActivityPresenter SUAPresenter;
    String from;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        ButterKnife.bind(this);
        if (getIntent().getExtras() != null) {
            from = getIntent().getStringExtra("from");
        }
        attachPresenter();
        bindEvents();
    }

    public void attachPresenter() {
        if (SUAPresenter == null) {
            SUAPresenter = new SignUpActivityPresenter();
        }
        SUAPresenter.onViewAttached(this, this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.signUpBtn:
                startSignUpProcess();
                break;
            case R.id.termsText:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://visym.com/legal.html"));
                startActivity(browserIntent);
                break;
            case R.id.loginText:
                Intent loginIntent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(loginIntent);
                finish();
                break;
            case R.id.goBackBtn:
                if (from.equalsIgnoreCase("login")) {
                    Intent frontScreenIntent = new Intent(SignUpActivity.this,
                            LoginActivity.class);
                    startActivity(frontScreenIntent);
                    finish();
                } else {
                    Intent frontScreenIntent = new Intent(SignUpActivity.this,
                            FrontScreenActivity.class);
                    startActivity(frontScreenIntent);
                    finish();
                }
                break;
        }
    }

    public void startSignUpProcess() {
        if (userFirstName.getText().toString().isEmpty() ||
                userLastName.getText().toString().isEmpty() ||
                userEmail.getText().toString().isEmpty() ||
                userPassword.getText().toString().isEmpty() ||
                userConfPassword.getText().toString().isEmpty()) {
            if (userFirstName.getText().toString().isEmpty() || userLastName.getText().toString().isEmpty()
                    || userEmail.getText().toString().isEmpty() || userPassword.getText().toString().isEmpty() ||
                    userConfPassword.getText().toString().isEmpty()) {
                displayErrorMessage("Please fill all the fields");
            }
        } else {
            String password = userPassword.getText().toString();
            String confirmPassword = userConfPassword.getText().toString();

            String validPassword = FieldValidator.isValidPassword(password);
            if (!TextUtils.isEmpty(validPassword)) {
                displayErrorMessage(validPassword);
                return;
            }

            String validPassword1 = FieldValidator.isValidPassword(confirmPassword);
            if (!TextUtils.isEmpty(validPassword1)) {
                displayErrorMessage(validPassword1);
                return;
            }
            if (!password.equals(confirmPassword)) {
                displayErrorMessage("New password and Confirm password does not matching");
                return;
            }
            Globals.showLoading(this);
            SUAPresenter.signUpApi(userFirstName.getText().toString(),
                    userLastName.getText().toString(), userEmail.getText().toString(),
                    userPassword.getText().toString());
        }
    }

    private void displayErrorMessage(String message) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Alert");
        dialog.setMessage(message);
        dialog.setPositiveButton("OK", (dialog1, which) -> {
            dialog1.dismiss();
        }).show();
    }

    @Override
    public void onSignUpSuccess(String email, boolean confirmationState) {
        runOnUiThread(() -> {
            Globals.dismissLoading();
            Intent redirectIntent = new Intent(SignUpActivity.this, EmailVerificationActivity.class);
            redirectIntent.putExtra("email", email);
            startActivity(redirectIntent);
            finish();
        });
    }

    @Override
    public void bindEvents() {
        termsText.setOnClickListener(this);
        loginText.setOnClickListener(this);
        signUpBtn.setOnClickListener(this);
        goBackBtn.setOnClickListener(this);

        userFirstName.addTextChangedListener(new EmptyFieldTextWatcher(userFirstNameLayout));
        userLastName.addTextChangedListener(new EmptyFieldTextWatcher(userLastNameLayout));
        userPassword.addTextChangedListener(new EmptyFieldTextWatcher(userPasswordLayout));
        userConfPassword.addTextChangedListener(new EmptyFieldTextWatcher(userConfPasswordLayout));
        userEmail.addTextChangedListener(new EmailFieldTextWatcher(userEmailLayout));
    }

    @Override
    public void onSignUpFailure(String errorMessage) {
        runOnUiThread(() -> {
            Globals.dismissLoading();
            if (!TextUtils.isEmpty(errorMessage)) {
                Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SignUpActivity.this, getString(R.string.unknown_error_message),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SUAPresenter.onViewDetached();
    }
}
