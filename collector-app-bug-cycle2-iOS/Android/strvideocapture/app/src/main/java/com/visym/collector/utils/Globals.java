package com.visym.collector.utils;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserState;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.S3ObjectManagerImplementation;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.android.material.snackbar.Snackbar;
import com.visym.collector.R;
import com.visym.collector.network.ConnectivityReceiver;
import com.visym.collector.usermodule.view.LoginActivity;

public class Globals extends Application {

    public static final String TAG = "STRVC";
    public static final String contentType = "application/x-www-form-urlencoded";
    private static Dialog pDialog;
    private static Dialog pDialog2;
    private static Context mcontext;
    private static Globals mInstance;
    private static Snackbar snackbar;
    public static AWSAppSyncClient mAWSAppSyncClient;
    public static boolean isSetupFlow = false;
    public static boolean isRetakeConsentVideo = false;


    @Override
    public void onCreate() {
        super.onCreate();
        mcontext = this;
        mInstance = this;

        initAmplify();
        initApiAuth();


        registerReceiver(new ConnectivityReceiver(),
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private void initAmplify() {
        AWSMobileClient.getInstance().initialize(getApplicationContext(), new Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails result) {
                boolean signedIn = AWSMobileClient.getInstance().isSignedIn();
                if (signedIn) {
                    registerUserState();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.d(TAG, "onError: ");
            }
        });
    }

    private void registerUserState() {
        AWSMobileClient.getInstance().addUserStateListener(details -> {
            UserState userState = details.getUserState();
            if (userState == UserState.SIGNED_OUT_USER_POOLS_TOKENS_INVALID) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
    }

    public void initApiAuth() {
        mAWSAppSyncClient = AWSAppSyncClient.builder()
                .context(mcontext)
                .awsConfiguration(new AWSConfiguration(mcontext))
                .s3ObjectManager(new S3ObjectManagerImplementation(new AmazonS3Client(AWSMobileClient.getInstance(),
                        Region.getRegion(Regions.US_EAST_1))))
                .cognitoUserPoolsAuthProvider(() -> {
                    try {
                        return AWSMobileClient.getInstance().getTokens().getIdToken().getTokenString();
                    } catch (Exception e) {
                        Log.d(TAG, "initApiAuth: " + e.getCause());
                        return null;
                    }
                }).build();
    }

    public static synchronized Globals getInstance() {
        return mInstance;
    }

    public void setConnectivityListener(ConnectivityReceiver.ConnectivityReceiverListener listener) {
        ConnectivityReceiver.connectivityReceiverListener = listener;
    }

    public static String getRefreshToken() {
        return new SharedPref(mcontext).getRefreshToken();
    }

    public static String getUserId() {
        return new SharedPref(mcontext).getRefreshToken();
    }

    public static String getAccessToken() {
        return new SharedPref(mcontext).getToken();
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

    public static void showLoading(Context ctx) {
        if (pDialog == null) {
            pDialog = new Dialog(ctx, R.style.TransparentProgressDialogWithPngImage);
            View mn = LayoutInflater.from(ctx).inflate(R.layout.loader, null);
            Window window = pDialog.getWindow();
            if (window != null) {
                window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                window.setBackgroundDrawableResource(R.color.transparent);
                pDialog.setContentView(mn);
                pDialog.setCancelable(false);
                pDialog.show();
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            }
        }
    }

    public static void dismissLoading() {
        if (pDialog != null && pDialog.isShowing()) {
            pDialog.dismiss();
            pDialog = null;
        }
    }

    public static boolean isShowingLoader() {
        boolean showing = false;
        if (pDialog != null && pDialog.isShowing()) {
            showing = pDialog.isShowing();
        }
        return showing;
    }

    public static void showLoading2(Context ctx) {
        if (pDialog2 == null) {
            pDialog2 = new Dialog(ctx, R.style.TransparentProgressDialogWithPngImage2);
            View mn = LayoutInflater.from(ctx).inflate(R.layout.loader2, null);
            pDialog2.getWindow().setBackgroundDrawableResource(R.color.transparent);
            pDialog2.setContentView(mn);
            pDialog2.setCancelable(false);
            pDialog2.show();
        }
    }

    public static void dismissLoading2() {
        if (pDialog2 != null && pDialog2.isShowing()) {
            pDialog2.dismiss();
            pDialog2 = null;
        }
    }

    public static boolean isShowingLoader2() {
        boolean showing2 = false;
        if (pDialog2 != null && pDialog2.isShowing()) {
            showing2 = pDialog2.isShowing();
        }
        return showing2;
    }

    public static void showSnackBar(String errorMsg, Context context, int length) {
        snackbar = Snackbar
                .make(((AppCompatActivity) context).findViewById(android.R.id.content), errorMsg, length);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(mcontext.getResources().getColor(R.color.darkBlack));
        TextView textView = (TextView) snackbarView.findViewById(
                com.google.android.material.R.id.snackbar_text);
        textView.setTextColor(mcontext.getResources().getColor(R.color.textLightColor));
        //textView.setTypeface(Typeface.create("poppinsregular",Typeface.NORMAL));
        snackbar.show();
    }

    public static Context getAppContext() {
        return mcontext;
    }

    public void unregisterConnectivityListener() {
        ConnectivityReceiver.connectivityReceiverListener = null;
    }

    public static AWSAppSyncClient getAppSyncClient() {
        return mAWSAppSyncClient;
    }
}
