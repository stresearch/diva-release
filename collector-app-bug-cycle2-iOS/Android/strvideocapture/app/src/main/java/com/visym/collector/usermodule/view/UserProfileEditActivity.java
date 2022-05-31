package com.visym.collector.usermodule.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.amazonaws.amplify.generated.graphql.UpdateStrCollectorMutation;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.results.UserCodeDeliveryDetails;
import com.amazonaws.services.cognitoidentityprovider.model.NotAuthorizedException;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.visym.collector.R;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.EmptyFieldTextWatcher;
import com.visym.collector.utils.FieldValidator;
import com.visym.collector.utils.Globals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import butterknife.BindView;
import butterknife.ButterKnife;
import type.UpdateStrCollectorInput;

public class UserProfileEditActivity extends AppCompatActivity implements View.OnClickListener {

    @BindView(R.id.userFirstName)
    TextInputEditText userFirstName;
    @BindView(R.id.userFirstNameLayout)
    TextInputLayout userFirstNameLayout;
    @BindView(R.id.userLastName)
    TextInputEditText userLastName;
    @BindView(R.id.userLastNameLayout)
    TextInputLayout userLastNameLayout;
    @BindView(R.id.userEmailLayout)
    TextInputLayout userEmailLayout;
    @BindView(R.id.userEmail)
    TextInputEditText userEmail;
    @BindView(R.id.changePasswordAction)
    TextView changePasswordAction;
    @BindView(R.id.backBtn)
    ImageView backBtn;
    @BindView(R.id.saveBtn)
    Button saveBtn;
    Context mcontext;

    private AppSharedPreference preference;
    private String email;
    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile_edit);
        ButterKnife.bind(this);
        mcontext = this;
        preference = AppSharedPreference.getInstance();

        bindEvents();
        initUserProfile();

    }

    private void initUserProfile() {
        String firstName = preference.readString(Constant.COLLECTOR_FNAME);
        if (!TextUtils.isEmpty(firstName)) {
            userFirstName.setText(firstName);
        }

        String lastName = preference.readString(Constant.COLLECTOR_LNAME);
        if (!TextUtils.isEmpty(lastName)) {
            userLastName.setText(lastName);
        }

        email = preference.readString(Constant.COLLECTOR_EMAIL);
        if (!TextUtils.isEmpty(email)) {
            userEmail.setText(email);
        }
    }

    private void bindEvents() {
        changePasswordAction.setOnClickListener(this);
        saveBtn.setOnClickListener(this);
        backBtn.setOnClickListener(this);
        userFirstName.addTextChangedListener(new EmptyFieldTextWatcher(userFirstNameLayout));
        userLastName.addTextChangedListener(new EmptyFieldTextWatcher(userLastNameLayout));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.changePasswordAction:
                displayPasswordChangeDialog();
                break;

            case R.id.saveBtn:
                userFirstNameLayout.setError(null);
                userLastNameLayout.setError(null);

                String firstName = userFirstName.getText().toString();
                if (TextUtils.isEmpty(firstName)) {
                    userFirstNameLayout.setError("First name should not empty");
                    userFirstNameLayout.setErrorEnabled(true);
                    return;
                }

                String lastName = userLastName.getText().toString();
                if (TextUtils.isEmpty(lastName)) {
                    userLastNameLayout.setError("First name should not empty");
                    userLastNameLayout.setErrorEnabled(true);
                    return;
                }

                String fname = FieldValidator.isValidName(firstName);
                if (!TextUtils.isEmpty(fname)) {
                    userFirstNameLayout.setError(fname);
                    userFirstNameLayout.setErrorEnabled(true);
                    return;
                }

                String lname = FieldValidator.isValidName(lastName);
                if (!TextUtils.isEmpty(lname)) {
                    userLastNameLayout.setError(lname);
                    userLastNameLayout.setErrorEnabled(true);
                    return;
                }
                showLoading();
                updateDetails(firstName, lastName);
                break;

            case R.id.backBtn:
                finish();
                break;
        }
    }

    private void displayPasswordChangeDialog() {
        View view = getLayoutInflater().inflate(R.layout.change_password_layout, null);
        TextInputEditText currentPasswordEditText = view.findViewById(R.id.current_password_edittext);
        TextInputEditText newPasswordEditText = view.findViewById(R.id.new_password_edittext);
        TextInputEditText confirmPasswordEditText = view.findViewById(R.id.confirm_password_edittext);
        TextView errorTextView = view.findViewById(R.id.error_textview);

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setView(view)
                .setPositiveButton("SAVE", (dialog, which) -> {

                }).setNegativeButton("CANCEL", (dialog, which) -> {

        });
        dialog = alert.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            errorTextView.setVisibility(View.GONE);
            String currentPassword = currentPasswordEditText.getText().toString();
            String newPassword = newPasswordEditText.getText().toString();
            String confirmPassword = confirmPasswordEditText.getText().toString();

            if (TextUtils.isEmpty(currentPassword) || TextUtils.isEmpty(newPassword)
                    || TextUtils.isEmpty(confirmPassword)) {
                errorTextView.setVisibility(View.VISIBLE);
                errorTextView.setText("Fields must not be empty");
                return;
            }

            String validPassword = FieldValidator.isValidPassword(newPassword);
            if (!TextUtils.isEmpty(validPassword)) {
                errorTextView.setVisibility(View.VISIBLE);
                errorTextView.setText(validPassword);
                return;
            }

            String validPassword1 = FieldValidator.isValidPassword(confirmPassword);
            if (!TextUtils.isEmpty(validPassword1)) {
                errorTextView.setVisibility(View.VISIBLE);
                errorTextView.setText(validPassword1);
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                errorTextView.setVisibility(View.VISIBLE);
                errorTextView.setText("New password and Confirm password does not matching");
                return;
            }

            updatePassword(currentPassword, newPassword);
        });

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            dismissDialog();
        });
    }

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            dialog = null;
        }
    }

    private void updatePassword(String currentPassword, String newPassword) {
        showLoading();
        AWSMobileClient.getInstance().changePassword(currentPassword, newPassword,
                new Callback<Void>() {
                    @Override
                    public void onResult(Void result) {
                        runOnUiThread(() -> {
                            dismissDialog();
                            dismissLoading();
                            Toast.makeText(UserProfileEditActivity.this,
                                    "Password changed successfully", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> {
                            dismissDialog();
                            dismissLoading();
                            if (e instanceof NotAuthorizedException) {
                                Toast.makeText(UserProfileEditActivity.this,
                                        "Incorrect current password. Please enter correct password", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(UserProfileEditActivity.this,
                                        e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                            dismissLoading();
                        });
                    }
                });
    }

    private void updateDetails(String firstName, String lastName) {
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("custom:first_name", firstName);
        attributes.put("custom:last_name", lastName);

        AWSMobileClient.getInstance().updateUserAttributes(attributes,
                new Callback<List<UserCodeDeliveryDetails>>() {
                    @Override
                    public void onResult(List<UserCodeDeliveryDetails> result) {
                        updateToDynamoDB(firstName, lastName);
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> {
                            dismissLoading();
                            Toast.makeText(UserProfileEditActivity.this,
                                    "Failed to updated profile", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void updateToDynamoDB(String firstName, String lastName) {
        UpdateStrCollectorInput devInput = UpdateStrCollectorInput.builder()
                .collector_id(preference.readString(Constant.COLLECTOR_ID_KEY))
                .collector_email(preference.readString(Constant.COLLECTOR_EMAIL))
                .first_name(firstName)
                .last_name(lastName).build();
        Globals.getAppSyncClient().mutate(UpdateStrCollectorMutation.builder().input(devInput).build())
                .enqueue(dataCallback);
    }

    private GraphQLCall.Callback<UpdateStrCollectorMutation.Data> dataCallback
            = new GraphQLCall.Callback<UpdateStrCollectorMutation.Data>() {
        @Override
        public void onResponse(@Nonnull Response<UpdateStrCollectorMutation.Data> response) {
            if (response != null && response.data() != null && response.data().updateStrCollector() != null) {

                runOnUiThread(() -> {
                    String firstName = response.data().updateStrCollector().first_name();
                    String lastName = response.data().updateStrCollector().last_name();

                    preference.storeValue(Constant.COLLECTOR_FNAME, firstName);
                    preference.storeValue(Constant.COLLECTOR_LNAME, lastName);

                    dismissLoading();
                    Toast.makeText(UserProfileEditActivity.this,
                            "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK);
                    finish();
                });
            } else {
                dismissLoading();

            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dismissLoading();
                    Toast.makeText(UserProfileEditActivity.this,
                            "Failed to update profile. Please try again", Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    public void dismissLoading() {
        if (Globals.isShowingLoader()) {
            Globals.dismissLoading();
        }
    }

    public void showLoading() {
        Globals.showLoading(this);
    }
}
