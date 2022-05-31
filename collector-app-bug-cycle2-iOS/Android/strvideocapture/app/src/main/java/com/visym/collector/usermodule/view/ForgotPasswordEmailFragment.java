package com.visym.collector.usermodule.view;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.visym.collector.R;
import com.visym.collector.usermodule.IUserModule;
import com.visym.collector.usermodule.presenter.ForgotPasswordActivityPresenter;
import com.visym.collector.utils.EmailFieldTextWatcher;
import com.visym.collector.utils.Globals;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;

public class ForgotPasswordEmailFragment extends Fragment implements View.OnClickListener,
        IUserModule.IForgotPasswordView {

    @BindView(R.id.goBackBtn)
    ImageView goBackBtn;

    @BindView(R.id.userEmail)
    TextInputEditText userEmail;

    @BindView(R.id.userEmailLayout)
    TextInputLayout userEmailLayout;

    @BindView(R.id.sendPasswordBtn)
    Button sendPasswordBtn;

    private IUserModule.IForgotPasswordPresenter fpPresenter;
    private FragmentAction listener;

    public static ForgotPasswordEmailFragment getInstance(FragmentAction listener) {
        ForgotPasswordEmailFragment fragment = new ForgotPasswordEmailFragment();
        fragment.setListener(listener);
        return fragment;
    }

    private void setListener(FragmentAction listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forgot_password_layout, container, false);
        ButterKnife.bind(this, view);
        attachPresenter();
        sendPasswordBtn.setOnClickListener(this);
        goBackBtn.setOnClickListener(this);
        userEmail.addTextChangedListener(new EmailFieldTextWatcher(userEmailLayout));
        return view;
    }

    public void attachPresenter() {
        if (fpPresenter == null) {
            fpPresenter = new ForgotPasswordActivityPresenter();
        }
        fpPresenter.onViewAttached(this, getActivity());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sendPasswordBtn:
                String email = userEmail.getText().toString();
                if (TextUtils.isEmpty(email)) {
                    userEmailLayout.setError(getResources().getString(R.string.isRequired));
                    userEmailLayout.setErrorEnabled(true);
                    return;
                }

                if (!Globals.isValidEmail(email)) {
                    userEmailLayout.setError(getResources().getString(R.string.invalid_email_message));
                    userEmailLayout.setErrorEnabled(true);
                    return;
                }

                Globals.showLoading(getActivity());
                fpPresenter.forgotPasswordApi(email);
                break;

            case R.id.goBackBtn:
                if (getActivity() == null) {
                    return;
                }
                getActivity().onBackPressed();
                break;
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fpPresenter.onViewDettached();
    }

    @Override
    public void onEmailSent(String emailAddress) {
        runOnUiThread(() -> {
            if (Globals.isShowingLoader()) {
                Globals.dismissLoading();
            }
            if (listener != null) {
                listener.navigateToNextFragment(emailAddress);
            }
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
                Globals.showSnackBar(getString(R.string.unknown_error_message),
                        getActivity(), Snackbar.LENGTH_SHORT);
            }
        });
    }

    public interface FragmentAction {
        void navigateToNextFragment(String emailAddress);
    }
}
