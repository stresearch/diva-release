package com.visym.collector.capturemodule.views;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amazonaws.amplify.generated.graphql.ListStrConsentQuestionnairesQuery;
import com.amazonaws.amplify.generated.graphql.SubjectByStrSubjectEmailQuery;
import com.amazonaws.amplify.generated.graphql.UpdateStrSubjectMutation;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.google.android.material.snackbar.Snackbar;
import com.visym.collector.R;
import com.visym.collector.usermodule.view.UserProfileActivity;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.Globals;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import javax.annotation.Nonnull;

import type.ModelSortDirection;
import type.UpdateStrSubjectInput;


public class EditConsentActivity extends AppCompatActivity implements View.OnClickListener {


    JSONArray consentQuestionArray, responseJsonArray, finalResponseArray;
    Spinner datasetRelease;
    Spinner retention;
    Spinner faceRecognitionUsage;
    Context mContext;
    JSONArray stringJarr;

    String collectorEmail;

    ImageView goBackBtn;
    TextView moreInfo;
    ArrayList<String> data_releaselist = new ArrayList<>();
    ArrayList<String> retentionlist = new ArrayList<>();
    ArrayList<String> face_recognitionlist = new ArrayList<>();
    ArrayAdapter<String> datasetReleaseAdapter;
    ArrayAdapter<String> retentionAdapter;
    ArrayAdapter<String> faceRecognitionUsageAdapter;
    Button editConsentResponseBtn, retakeConsentVideo;
    private Dialog actionDialog;
    private AppSharedPreference preference;

    private String DATASET_RESPONSE_KEY = "DataSetResponse";
    private String FACE_RECOGNITION_KEY = "FaceRecognition";
    private String RETENTION_KEY = "Retention";
    private GraphQLCall.Callback<SubjectByStrSubjectEmailQuery.Data> SubjectBySubjectByEmailQueryvalue = new GraphQLCall.Callback<SubjectByStrSubjectEmailQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<SubjectByStrSubjectEmailQuery.Data> response) {
            if (response.data() != null && response.data().subjectByStrSubjectEmail() != null) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Globals.isShowingLoader()) {
                            Globals.dismissLoading();
                        }
                        if (response.data().subjectByStrSubjectEmail().items().size() > 0) {
                            try {
                                JSONObject jb = new JSONObject(String.valueOf(response.data().subjectByStrSubjectEmail().items().get(0).consent_response()));
                                stringJarr = jb.getJSONArray("platform");
                                collectorEmail = response.data().subjectByStrSubjectEmail().items().get(0).collector_email();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (stringJarr != null) {
                            for (int i = 0; i < stringJarr.length(); i++) {
                                try {
                                    if (stringJarr.getJSONObject(i).has("q_category")) {
                                        String qCategory = stringJarr.getJSONObject(i).getString("q_category");
                                        if (!TextUtils.isEmpty(qCategory)
                                                && (qCategory.equals(DATASET_RESPONSE_KEY) || qCategory.equals("Data Release"))) {
                                            data_releaselist.add(stringJarr.getJSONObject(i).getString("q_category_response"));
                                            if (stringJarr.getJSONObject(i).getString("response").equals("true")) {
                                                int spinnerPosition = datasetReleaseAdapter.getPosition(stringJarr.getJSONObject(i)
                                                        .getString("q_category_response"));
                                                datasetRelease.setSelection(spinnerPosition);
                                                datasetReleaseAdapter.notifyDataSetChanged();
                                            }
                                        } else if (!TextUtils.isEmpty(qCategory)
                                                && (qCategory.equals(FACE_RECOGNITION_KEY) || qCategory.equals("Face Recognition"))) {
                                            face_recognitionlist.add("No");
                                            face_recognitionlist.add(stringJarr.getJSONObject(i).getString("q_category_response"));
                                            if (stringJarr.getJSONObject(i).getString("response").equals("true")) {
                                                int spinnerPosition = faceRecognitionUsageAdapter.getPosition(stringJarr.getJSONObject(i)
                                                        .getString("q_category_response"));
                                                faceRecognitionUsage.setSelection(spinnerPosition);
                                                faceRecognitionUsageAdapter.notifyDataSetChanged();
                                            }

                                        } else if (!TextUtils.isEmpty(qCategory) && qCategory.equals(RETENTION_KEY)) {
                                            retentionlist.add(stringJarr.getJSONObject(i).getString("q_category_response"));
                                            if (stringJarr.getJSONObject(i).getString("response").equals("true")) {
                                                int spinnerPosition = retentionAdapter.getPosition(stringJarr.getJSONObject(i)
                                                        .getString("q_category_response"));
                                                retention.setSelection(spinnerPosition);
                                                retentionAdapter.notifyDataSetChanged();
                                            }
                                        }
                                    }

                                } catch (Exception e) {
                                    Log.e(Globals.TAG, "onResponsek: " + e.toString());
                                }
                            }
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                datasetReleaseAdapter = new ArrayAdapter<String>(EditConsentActivity.this,
                                        android.R.layout.simple_spinner_item, data_releaselist);
                                retentionAdapter = new ArrayAdapter<String>(EditConsentActivity.this,
                                        android.R.layout.simple_spinner_item, retentionlist);
                                faceRecognitionUsageAdapter = new ArrayAdapter<String>(EditConsentActivity.this,
                                        android.R.layout.simple_spinner_item, face_recognitionlist);


                                datasetReleaseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                datasetRelease.setAdapter(datasetReleaseAdapter);

                                datasetReleaseAdapter.notifyDataSetChanged();

                                retentionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                retention.setAdapter(retentionAdapter);
                                retentionAdapter.notifyDataSetChanged();


                                faceRecognitionUsageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                faceRecognitionUsage.setAdapter(faceRecognitionUsageAdapter);
                                faceRecognitionUsageAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Globals.isShowingLoader()) {
                            Globals.dismissLoading();
                        }
                    }
                });
            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Globals.isShowingLoader()) {
                        Globals.dismissLoading();
                    }
                    Globals.showSnackBar(mContext.getResources().getString(R.string.unknown_error_message),
                            mContext, Snackbar.LENGTH_LONG);
                }
            });
        }
    };
    private GraphQLCall.Callback<UpdateStrSubjectMutation.Data> updateSubResponse = new GraphQLCall.Callback<UpdateStrSubjectMutation.Data>() {
        @Override
        public void onResponse(@Nonnull Response<UpdateStrSubjectMutation.Data> response) {

            if (response != null && response.data() != null && response.data().updateStrSubject() != null) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Globals.isShowingLoader()) {
                            Globals.dismissLoading();
                        }
                        Toast.makeText(EditConsentActivity.this, "Consent updated", Toast.LENGTH_LONG).show();
                        Intent redirectUserProfile = new Intent(EditConsentActivity.this, UserProfileActivity.class);
                        startActivity(redirectUserProfile);
                        finish();
                    }
                });
                Log.e(Globals.TAG, "onResponse: " + response.data().updateStrSubject().consent_response());
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Globals.isShowingLoader()) {
                            Globals.dismissLoading();
                        }
                    }
                });
            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Globals.isShowingLoader()) {
                        Globals.dismissLoading();
                    }
                }
            });
            Log.e(Globals.TAG, "onResponse: failure " + e.toString());

        }
    };
    private GraphQLCall.Callback<ListStrConsentQuestionnairesQuery.Data> consentQuestions = new GraphQLCall.Callback<ListStrConsentQuestionnairesQuery.Data>() {
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
                        consentQuestionArray.put(itemObject);


                    } catch (Exception e) {
                        Log.e(Globals.TAG, "onResponse: " + e.toString());
                    }
                }
            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Globals.showSnackBar(mContext.getResources().getString(R.string.unknown_error_message),
                            mContext, Snackbar.LENGTH_LONG);
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_consent);
        preference = AppSharedPreference.getInstance();
        datasetRelease = (Spinner) findViewById(R.id.datasetRelease);
        retention = (Spinner) findViewById(R.id.retention);
        faceRecognitionUsage = (Spinner) findViewById(R.id.faceRecognitionUsage);
        goBackBtn = findViewById(R.id.goBackBtn);
        editConsentResponseBtn = (Button) findViewById(R.id.editConsentResponseBtn);
        moreInfo = (TextView) findViewById(R.id.moreInfo);
        retakeConsentVideo = (Button) findViewById(R.id.retakeConsentBtn);
        mContext = this;
        responseJsonArray = new JSONArray();
        finalResponseArray = new JSONArray();
        datasetRelease.setSelection(-1);
        retention.setSelection(-1);
        faceRecognitionUsage.setSelection(-1);

        Globals.showLoading(mContext);
        Globals.mAWSAppSyncClient.query(ListStrConsentQuestionnairesQuery.builder()
                .project_id(preference.readString(Constant.PROJECT_ID_KEY))
                .sortDirection(ModelSortDirection.ASC).build())
                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                .enqueue(consentQuestions);

        Globals.getAppSyncClient().query(SubjectByStrSubjectEmailQuery.builder()
                .subject_email(preference.readString(Constant.COLLECTOR_EMAIL))
                .build())
                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                .enqueue(SubjectBySubjectByEmailQueryvalue);

        editConsentResponseBtn.setOnClickListener(this);
        goBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        moreInfo.setOnClickListener(this);
        retakeConsentVideo.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.editConsentResponseBtn) {
            for (int i = 0; i < consentQuestionArray.length(); i++) {
                try {
                    if (consentQuestionArray.getJSONObject(i).has("q_category")) {
                        Log.e(Globals.TAG, "onClick: id " + consentQuestionArray.getJSONObject(i).getString("id"));
                        if (consentQuestionArray.getJSONObject(i).getString("q_category").equalsIgnoreCase("Data Release")
                                || consentQuestionArray.getJSONObject(i).getString("q_category").equals(DATASET_RESPONSE_KEY)) {
                            if (consentQuestionArray.getJSONObject(i).getString("q_category_response").equalsIgnoreCase(datasetRelease.getSelectedItem().toString())) {
                                Log.e(Globals.TAG, "onClick: consent Data Release  " + consentQuestionArray.getJSONObject(i));
                                Log.e(Globals.TAG, "onClick: consent Data Release id " + consentQuestionArray.getJSONObject(i).getString("id"));
                                JSONObject res = new JSONObject();
                                res.put("id", consentQuestionArray.getJSONObject(i).getString("id"));
                                res.put("response", "true");
                                res.put("q_category", consentQuestionArray.getJSONObject(i).getString("q_category"));
                                res.put("q_category_response", consentQuestionArray.getJSONObject(i).getString("q_category_response"));

                                responseJsonArray.put(res);
                            } else {
                                JSONObject res = new JSONObject();
                                res.put("id", consentQuestionArray.getJSONObject(i).getString("id"));
                                res.put("response", "false");
                                res.put("q_category", consentQuestionArray.getJSONObject(i).getString("q_category"));
                                res.put("q_category_response", consentQuestionArray.getJSONObject(i).getString("q_category_response"));

                                responseJsonArray.put(res);
                            }

                        } else if (consentQuestionArray.getJSONObject(i).getString("q_category").equalsIgnoreCase(RETENTION_KEY)) {
                            if (consentQuestionArray.getJSONObject(i).getString("q_category_response").equalsIgnoreCase(retention.getSelectedItem().toString())) {
                                Log.e(Globals.TAG, "onClick: consent Retentions " + consentQuestionArray.getJSONObject(i));
                                Log.e(Globals.TAG, "onClick: consent  Retentions id " + consentQuestionArray.getJSONObject(i).getString("id"));
                                JSONObject res = new JSONObject();
                                res.put("id", consentQuestionArray.getJSONObject(i).getString("id"));
                                res.put("response", "true");
                                res.put("q_category", consentQuestionArray.getJSONObject(i).getString("q_category"));
                                res.put("q_category_response", consentQuestionArray.getJSONObject(i).getString("q_category_response"));

                                responseJsonArray.put(res);
                            } else {
                                JSONObject res = new JSONObject();
                                res.put("id", consentQuestionArray.getJSONObject(i).getString("id"));
                                res.put("response", "false");
                                res.put("q_category", consentQuestionArray.getJSONObject(i).getString("q_category"));
                                res.put("q_category_response", consentQuestionArray.getJSONObject(i).getString("q_category_response"));

                                responseJsonArray.put(res);
                            }
                        } else if (consentQuestionArray.getJSONObject(i).getString("q_category").equalsIgnoreCase("Face Recognition")
                                || consentQuestionArray.getJSONObject(i).getString("q_category").equals(FACE_RECOGNITION_KEY)) {
                            if (consentQuestionArray.getJSONObject(i).getString("q_category_response").equalsIgnoreCase(faceRecognitionUsage.getSelectedItem().toString())) {
                                Log.e(Globals.TAG, "onClick: consent FaceRecognition " + consentQuestionArray.getJSONObject(i));
                                Log.e(Globals.TAG, "onClick: consent  FaceRecognition id " + consentQuestionArray.getJSONObject(i).getString("id"));
                                JSONObject res = new JSONObject();
                                res.put("id", consentQuestionArray.getJSONObject(i).getString("id"));
                                res.put("response", "true");
                                res.put("q_category", consentQuestionArray.getJSONObject(i).getString("q_category"));
                                res.put("q_category_response", consentQuestionArray.getJSONObject(i).getString("q_category_response"));

                                responseJsonArray.put(res);
                            } else {
                                JSONObject res = new JSONObject();
                                res.put("id", consentQuestionArray.getJSONObject(i).getString("id"));
                                res.put("response", "true");
                                res.put("q_category", consentQuestionArray.getJSONObject(i).getString("q_category"));
                                res.put("q_category_response", "No");
                                responseJsonArray.put(res);
                            }
                        }
                    } else {
                        JSONObject res = new JSONObject();
                        res.put("id", consentQuestionArray.getJSONObject(i).getString("id"));
                        res.put("response", "false");
                        responseJsonArray.put(res);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
            if (collectorEmail != null) {
                Globals.showLoading(this);

                JSONObject res = new JSONObject();
                try {
                    res.put("response", responseJsonArray);
                    //finalResponseArray.put(res);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                UpdateStrSubjectInput sub = UpdateStrSubjectInput.builder()
                        .collector_email(collectorEmail)
                        .subject_email(collectorEmail)
                        .consent_response(String.valueOf(res))
                        .build();

                Globals.mAWSAppSyncClient.mutate(UpdateStrSubjectMutation.builder()
                        .input(sub).build())
                        .enqueue(updateSubResponse);
            }
        }
        if (v.getId() == R.id.moreInfo) {
            actionDialog = new Dialog(this);
            actionDialog.setContentView(R.layout.webview_more_info);
            actionDialog.setTitle("More Info");

            actionDialog.getWindow()
                    .setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            WebView moreInfoWebView = actionDialog.findViewById(R.id.moreInfoWebView);
            moreInfoWebView.loadUrl("file:///android_asset/consentmoreinfo.html");
            ImageView dismissDialog = actionDialog.findViewById(R.id.closeBtn);
            dismissDialog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    actionDialog.dismiss();
                }
            });
            actionDialog.show();
        }

        if (v.getId() == R.id.retakeConsentBtn) {
            Intent videoCaptureIntent = new Intent(EditConsentActivity.this,
                    ConsentVideoCaptureActivity.class);
            videoCaptureIntent.putExtra("isRetakeVideo", true);
            startActivity(videoCaptureIntent);
        }
    }

    @Override
    protected void onDestroy() {
        if (Globals.isShowingLoader()) {
            Globals.dismissLoading();
        }
        super.onDestroy();
    }
}

