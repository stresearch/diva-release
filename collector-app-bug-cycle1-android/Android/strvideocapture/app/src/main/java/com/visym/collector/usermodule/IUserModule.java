package com.visym.collector.usermodule;

import android.content.Context;

public interface IUserModule {

  interface ILoginView {

    void bindEvents();

    void redirectUserSetupActivity();

    void redirectEmailVerificationActivity(String username);

    void onRequestFailure(String errorMessage);

    void redirectToResetPassword(String email);
  }

  interface ILoginPresenter {

    void onViewAttached(IUserModule.ILoginView mview, Context context);

    void onViewDetached();

    void loginApi(String useremail, String password);
  }

  interface IForgotPasswordView {
    void onEmailSent(String emailAddress);

    void onFailure(String errorMessage);
  }

  interface IForgotPasswordPresenter {

    void onViewAttached(IUserModule.IForgotPasswordView mview, Context context);

    void onViewDettached();

    void forgotPasswordApi(String emailAddress);

  }

  interface IPasswordResetView {

    void onEmailSent(String email);

    void onFailure(String errorMessage);

    void onResetPasswordSuccess();
  }

  interface IPasswordResetPresenter {

    void resendOTP(String email);

    void resetPassword(String otpText, String newPass);
  }


  interface ISignUpView {

    void onSignUpSuccess(String email, boolean confirmationState);

    void bindEvents();

    void onSignUpFailure(String errorMessage);
  }

  interface ISignUpPresenter {

    void onViewAttached(IUserModule.ISignUpView mview, Context context);

    void onViewDetached();

    void signUpApi(String firstName, String lastName, String email, String password);

  }


  interface IUserProfileView {

    void displayAccountInformation();

    void displayStatsInformation();

    void redirectToTheFrontScreen();

    void bindEvents();

    void onLogoutFailure(String errorMessage);

    void refreshActivity();
  }

  interface IUserProfilePresenter {

    void onViewAttached(IUserModule.IUserProfileView mview, Context context);

    void onViewDetached();

    void signOutFunctionality();

    void revokeUserConsentForTheDay();
  }

}
