package com.visym.collector.usermodule.view;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.visym.collector.R;
import com.visym.collector.usermodule.IUserModule;
import com.visym.collector.usermodule.presenter.PasswordResetPresenter;
import com.visym.collector.utils.Globals;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;

public class ForgotPasswordResetFragment extends Fragment implements View.OnClickListener, IUserModule.IPasswordResetView {

    @BindView(R.id.email_id_textview)
    TextView emailIdTextView;
    @BindView(R.id.back_button)
    ImageView backButton;
    @BindView(R.id.otpEditText)
    TextInputEditText otpEditText;
    @BindView(R.id.newPasswordEmail)
    TextInputEditText newPassEditText;
    @BindView(R.id.confirmPasswordEmail)
    TextInputEditText confirmPassEditText;
    @BindView(R.id.resend_otp_textview)
    TextView resendOtpTextView;
    @BindView(R.id.change_password_text)
    TextView changePasswordTextView;
    @BindView(R.id.error_textview)
    TextView errorTextView;
    private String email;
    private IUserModule.IPasswordResetPresenter presenter;

    public static ForgotPasswordResetFragment getInstance(String email) {
        ForgotPasswordResetFragment fragment = new ForgotPasswordResetFragment();
        Bundle bundle = new Bundle();
        bundle.putString("email", email);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onEmailSent(String email) {
        runOnUiThread(() -> {
            if (getActivity() == null) {
                return;
            }
            if (Globals.isShowingLoader()) {
                Globals.dismissLoading();
            }
            resendOtpTextView.setText(getActivity().getString(R.string.otp_sent_hint));
            resendOtpTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_done_icon, 0, 0, 0);
        });
    }

    @Override
    public void onFailure(String errorMessage) {
        runOnUiThread(() -> {
            if (Globals.isShowingLoader()) {
                Globals.dismissLoading();
            }
            if (!TextUtils.isEmpty(errorMessage)) {
                Globals.showSnackBar(errorMessage, getActivity(), Snackbar.LENGTH_SHORT);
            } else {
                Globals.showSnackBar(getString(R.string.unknown_error_message), getActivity(), Snackbar.LENGTH_SHORT);
            }
        });
    }

    @Override
    public void onResetPasswordSuccess() {
        runOnUiThread(() -> {
            if (Globals.isShowingLoader()) {
                Globals.dismissLoading();
            }
            if (getActivity() != null) {
                Toast.makeText(getActivity(), "Password changed successfully", Toast.LENGTH_SHORT).show();
                getActivity().onBackPressed();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.reset_password_fragment_layout, container, false);
        ButterKnife.bind(this, view);

        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey("email")) {
            email = arguments.getString("email");
            init();
        }
        presenter = new PasswordResetPresenter(getActivity(), this);
        return view;
    }

    public void init() {
        try {
            String startChar = email.subSequence(0, 1).toString();
            String fName = email.split("@")[0];
            String endChar = fName.substring(fName.length() - 1);
            String extension = email.split("@")[1];
            String separator = "****";

            String finalString = startChar + separator + endChar + "@" + extension;
            if (!TextUtils.isEmpty(finalString)) {
                emailIdTextView.setText(String.format("%s\n%s", getString(R.string.otp_sent_hint), finalString));
            }
        } catch (Exception e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        resendOtpTextView.setOnClickListener(this);
        changePasswordTextView.setOnClickListener(this);
        backButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        errorTextView.setVisibility(View.GONE);
        switch (v.getId()) {
            case R.id.back_button:
                if (getActivity() == null) {
                    return;
                }
                getActivity().onBackPressed();
                break;

            case R.id.resend_otp_textview:
                errorTextView.setVisibility(View.GONE);
                Globals.showLoading(getActivity());
                presenter.resendOTP(email);
                break;

            case R.id.change_password_text:
                String otpText = otpEditText.getText().toString();
                if (TextUtils.isEmpty(otpText)) {
                    errorTextView.setVisibility(View.VISIBLE);
                    errorTextView.setText("OTP must not be empty");
                    return;
                }

                String newPass = newPassEditText.getText().toString();
                if (TextUtils.isEmpty(newPass)) {
                    errorTextView.setVisibility(View.VISIBLE);
                    errorTextView.setText("Password field must not be empty");
                    return;
                }

                String confirmPass = confirmPassEditText.getText().toString();
                if (TextUtils.isEmpty(confirmPass)) {
                    errorTextView.setVisibility(View.VISIBLE);
                    errorTextView.setText("Password field must not be empty");
                    return;
                }

                if (!newPass.contentEquals(confirmPass)) {
                    errorTextView.setVisibility(View.VISIBLE);
                    errorTextView.setText("Password fields are not matching");
                    return;
                }
                Globals.showLoading(getActivity());
                presenter.resetPassword(otpText, newPass);
                break;
        }
    }

    public interface ResetFragmentAction {

    }
}
