package com.visym.collector.capturemodule.presenters;

import android.content.Context;
import android.util.Log;

 import com.amazonaws.amplify.generated.graphql.ListStrConsentQuestionnairesQuery;
 import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.visym.collector.R;
import com.visym.collector.capturemodule.ICaptureModule;
import com.visym.collector.capturemodule.ICaptureModule.IConsentConfirmationView;
import com.visym.collector.network.ApiClient;
import com.visym.collector.network.ApiInterface;
import com.visym.collector.network.ConnectivityReceiver;
import com.visym.collector.network.ErrorHandler;
import com.visym.collector.network.IErrorInterator;
import com.visym.collector.network.NetworkRequest;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.Globals;
import com.visym.collector.utils.SharedPref;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;

import type.ModelSortDirection;

import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;

public class ConsentConfirmationPresenter implements ICaptureModule.IConsentConfirmationPresent,
    IErrorInterator {

  private ICaptureModule.IConsentConfirmationView mview;
  private Context mcontext;
  private ApiInterface api;
  private NetworkRequest networkRequest;
  private SharedPref sharedPref;
  private ErrorHandler errorHandler;
  JSONArray consentQuestionArray;

  @Override
  public void onViewAttached(IConsentConfirmationView view, Context context) {
      this.mview = view;
      this.mcontext = context;
      this.api = ApiClient.buildService();
      this.networkRequest = new NetworkRequest();
      this.sharedPref = new SharedPref(context);
      this.errorHandler = new ErrorHandler(context,this);
  }

  @Override
  public void onViewDetached() {
    this.mview = null;
  }

  @Override
  public void getConsentDatas(String version) {


    Globals.getAppSyncClient().query(ListStrConsentQuestionnairesQuery.builder()
            .project_id(AppSharedPreference.getInstance().readString(Constant.PROJECT_ID_KEY))
            .sortDirection(ModelSortDirection.ASC).build())
            .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
            .enqueue(qData);
  }

  private GraphQLCall.Callback<ListStrConsentQuestionnairesQuery.Data> qData = new GraphQLCall.Callback<ListStrConsentQuestionnairesQuery.Data>() {
    @Override
    public void onResponse(@Nonnull Response<ListStrConsentQuestionnairesQuery.Data> response) {
      consentQuestionArray = new JSONArray();
      if (response != null && response.data() != null && response.data().listStrConsentQuestionnaires() != null) {

      for (int i = 0; i < response.data().listStrConsentQuestionnaires().items().size(); i++) {
        JSONObject itemObject = new JSONObject();
        try {
          itemObject.put("id", response.data().listStrConsentQuestionnaires().items().get(i).id());
          if (response.data().listStrConsentQuestionnaires().items().get(i).agree_question_id() != null) {
            itemObject.put("agree_question_id", response.data().listStrConsentQuestionnaires().items().get(i).agree_question_id());
          }
          if (response.data().listStrConsentQuestionnaires().items().get(i).disagree_question_id() != null) {
            itemObject.put("disagree_question_id", response.data().listStrConsentQuestionnaires().items().get(i).disagree_question_id());
          }
          itemObject.put("short_description", response.data().listStrConsentQuestionnaires().items().get(i).short_description());
          itemObject.put("q_category", response.data().listStrConsentQuestionnaires().items().get(i).category());
          itemObject.put("q_category_response", response.data().listStrConsentQuestionnaires().items().get(i).category_response());
          itemObject.put("more_info", response.data().listStrConsentQuestionnaires().items().get(i).more_info());
          consentQuestionArray.put(itemObject);

        } catch (Exception e) {
          Log.e(Globals.TAG, "onResponse: " + e.toString());
        }
      }

      runOnUiThread(new Runnable() {

        @Override
        public void run() {
          // Stuff that updates the UI
          mview.initConsentDataObjects(consentQuestionArray);
        }
      });
      Log.e(Globals.TAG, "onResponse: " + response.data().listStrConsentQuestionnaires().items().toString());
    }else {
        runOnUiThread(new Runnable() {

          @Override
          public void run() {
            // Stuff that updates the UI
            mview.initConsentDataObjects(consentQuestionArray);
          }
        });
      }
    }

    @Override
    public void onFailure(@Nonnull ApolloException e) {
      Log.e(Globals.TAG, "onFailure: "+e.toString());
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          mview.displayRequestFailed();
        }
      });
    }
  };


  public void getUserConsentDatas(String emailId) {
    if (ConnectivityReceiver.isConnected()) {
      Globals.showLoading(mcontext);
      networkRequest.performAsyncRequest1(api.getUserConsent(emailId,sharedPref.getUserId()), (data) -> {
        Gson gson = new Gson();
        try {
        JSONObject consentDatas = new JSONObject(gson.toJson(data.getAsJsonObject("data")));
          //mview.initConsentDataObjects(consentDatas);

          //if user consent is present, and its lattest one then display it, else get the consent from
          // getConsentDatas();
        } catch (JSONException e) {
          e.toString();
        }
      }, (error) -> {
        Log.e(Globals.TAG, "getConsentDatas: error " + error.getMessage());
      }, errorHandler);
    } else {
      Globals
          .showSnackBar(mcontext.getResources().getString(R.string.noInternet), mcontext, Snackbar.LENGTH_LONG);
    }
  }

  @Override
  public void dismissLoading() {
    Globals.dismissLoading();
  }

  @Override
  public void showLoading() {
    Globals.showLoading(mcontext);
  }
}
