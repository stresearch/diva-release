package com.visym.collector.network;

import android.util.Log;

import com.visym.collector.model.NetworkCallback;
import com.visym.collector.model.UserModel;
import com.visym.collector.utils.Globals;

import org.jetbrains.annotations.NotNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;
import rx.Observable;
import rx.Single;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class NetworkRequest {
    private static NetworkRequest networkRequest;

    private Observable<ServerResponse<UserModel>> refreshTokenObservable = null;

    public static NetworkRequest getInstance() {
        if (networkRequest == null) {
            networkRequest = new NetworkRequest();
        }
        return networkRequest;
    }

    public NetworkRequest() {
        initRefreshTokenObservable();
    }

    public <T> Subscription performAsyncRequest1(Observable<T> observable, Action1<? super T> onAction, Action1<Throwable> onError, ErrorHandler errorHandler) {
        // Specify a scheduler (Scheduler.newThread(), Scheduler.immediate(), ...)
        // We choose Scheduler.io() to perform network request in a thread pool
        //currentObservable = PublishSubject.create();
        errorHandler.setRetrySubject(PublishSubject.create());

        return observable.subscribeOn(Schedulers.io())
                .onErrorResumeNext(refreshTokenAndRetry(observable))
                .doOnError(err -> {
                    errorHandler.handelError(err);
                })
                .retryWhen(retryHandler -> retryHandler.flatMap(nothing -> errorHandler.getRetrySubject().asObservable()))
                .doOnCompleted(() -> errorHandler.dismissView())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onAction, onError);
    }

    public <T> Subscription performAsyncRequest(Observable<T> observable, Action1<? super T> onAction, Action1<Throwable> onError) {
        // Specify a scheduler (Scheduler.newThread(), Scheduler.immediate(), ...)
        // We choose Scheduler.io() to perform network request in a thread pool

        return observable.subscribeOn(Schedulers.io())
                 .onErrorResumeNext(refreshTokenAndRetry(observable))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onAction, onError);
    }

    private <T> Func1<Throwable, ? extends Observable<? extends T>> refreshTokenAndRetry(final Observable<T> toBeResumed) {
        return new Func1<Throwable, Observable<? extends T>>() {
            @Override
            public Observable<? extends T> call(Throwable throwable) {
                // Here check if the error thrown really is a 401
                int code = ((HttpException) throwable).response().code();
                if (code == 401 || code == 500) {
                    synchronized (this) {
                        if (refreshTokenObservable == null) {
                            if (Globals.getRefreshToken() != null)
                                initRefreshTokenObservable();
                            else return Observable.error(throwable);
                        }

                        // we have to fetch the token, and return the result other threads
                        return refreshTokenObservable.flatMap(new Func1<ServerResponse<UserModel>, Observable<? extends T>>() {
                            @Override
                            public Observable<? extends T> call(ServerResponse<UserModel> agentServerResponse) {
                                if (agentServerResponse.isSuccess())
                                    return toBeResumed;
                                else return Observable.error(throwable);
                            }
                        });
                    }

                }
                // re-throw this error because it's not recoverable from here
                return Observable.error(throwable);
            }
        }

                ;
    }

    public void initRefreshTokenObservable() {
        if (Globals.getRefreshToken() == null)
            return;
        refreshTokenObservable = Single.defer(() -> refreshAccessToken())
                .toObservable()
                .share()
                .doOnCompleted(() -> initRefreshTokenObservable());
    }

    private Single<ServerResponse<UserModel>> refreshAccessToken() {
        return ApiClient.buildService().getRefreshToken(Globals.getRefreshToken(),Globals.getUserId())
                .doOnSuccess(response -> {
                    if (response.isSuccess()) {

                            UserModel userObject =  response.getData();
                        Log.e(Globals.TAG, "refreshAccessToken: "+userObject.toString());
                     // Save data to shared preferences
                    }
                })
                .doOnError(throwable -> {
                    //Clear credentials
                    //on token refresh failed
                });
    }

    public <T> void requestCall(Call<T> requestCall, NetworkCallback<T> responseCallback) {
        requestCall.enqueue(new Callback<T>() {
            @Override
            public void onResponse(@NotNull Call<T> call, @NotNull Response<T> response) {
                if (response.isSuccessful()){
                    responseCallback.onSuccess(response.body());
                }else {
                    responseCallback.onFailure(new NetworkClientError(response));
                }
            }

            @Override
            public void onFailure(@NotNull Call<T> call, @NotNull Throwable t) {
                responseCallback.onFailure(new NetworkClientError(t));
            }
        });
    }


}
