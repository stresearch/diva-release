package com.visym.collector.network;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import com.visym.collector.utils.Globals;
import java.net.UnknownHostException;

import retrofit2.HttpException;
import rx.subjects.PublishSubject;

public class ErrorHandler {

    private Snackbar snackbar;
    private PublishSubject<Void> retrySubject;
    private Context context;
    private IErrorInterator interator;
    private Boolean canLoad = false;
    private boolean connectionFailed;

    public ErrorHandler(Context context, IErrorInterator interator) {
        this.context = context;
        this.interator = interator;
        this.connectionFailed = false;
    }
    public void handelError(Throwable error) {
        interator.dismissLoading();
      //  Log.e(Globals.TAG, "handelError: 22"+error.getCause().getMessage());
        if (error instanceof HttpException) {
            Log.e(Globals.TAG, "handelError: "+((HttpException) error).response());
            int code = ((HttpException) error).response().code();
            String msg = ((HttpException) error).response().message();
            Log.e(Globals.TAG, "handelError: "+code + " Error Message " +msg);
            if(Globals.isShowingLoader()){
                Globals.dismissLoading();
            }
            switch (code) {
                case 401:
                    msg = "message from string value ";//context.getString(R.string.error_unauthorized_user);
                    showLogoutSnackBar(msg, context);
                    break;
                 case 400:
                     msg = "Bad Request";
                     showLogoutSnackBar(msg, context);
                    break;
                case 413:
                    showLogoutSnackBar("Entity Too Large", context);
                    break;
                case 204:
                    showLogoutSnackBar("Entity Too Large", context);
                    break;
                case 502:
                    showLogoutSnackBar("Bad Gateway", context);
                    break;
                case 504:
                    showLogoutSnackBar("Gateway Timeout", context);
                    break;
                default:
                    retrySubject.onError(error);
                    break;
            }
        }
        else if (error instanceof UnknownHostException || error instanceof NoConnectivityException) {
            setConnectionFailed(true);
            Log.e(Globals.TAG, "handelError: unknownhost "+error.toString());
            showSnackBar("TextErrorMessage", context);
        }
    }

    public  void showSnackBar(String errorMsg, final Context context) {
        snackbar = Snackbar
                .make(((AppCompatActivity) context).findViewById(android.R.id.content), errorMsg, Snackbar.LENGTH_INDEFINITE)
                .setActionTextColor(Color.RED)
                .setAction("RETRY", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (canLoad)
                            interator.showLoading();
                        setConnectionFailed(false);
                        retrySubject.onNext(null);
                    }
                });
        snackbar.show();
    }

    private void showLogoutSnackBar(String errorMsg, final Context context) {
        snackbar = Snackbar
                .make(((AppCompatActivity) context).findViewById(android.R.id.content), errorMsg, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    public void setConnectionFailed(boolean connectionFailed) {
        this.connectionFailed = connectionFailed;
    }

    public Boolean getConnectionFailed() {
        return connectionFailed;
    }

    public void setRetrySubject(PublishSubject<Void> retrySubject) {
        this.retrySubject = retrySubject;
    }

    public void dismissView() {
        if (snackbar != null) {
            snackbar.dismiss();
        }
        interator.dismissLoading();

    }
    public PublishSubject<Void> getRetrySubject() {
        return retrySubject;
    }


}
