package com.visym.collector.capturemodule.views;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amazonaws.amplify.generated.graphql.SubjectByStrSubjectEmailQuery;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.visym.collector.R;
import com.visym.collector.capturemodule.ICaptureModule;
import com.visym.collector.capturemodule.adapters.ConsentRecordListAdapter;
import com.visym.collector.capturemodule.adapters.ConsentSubjectListAdapter;
import com.visym.collector.capturemodule.interfaces.Listeners;
import com.visym.collector.capturemodule.model.DataModel;
import com.visym.collector.capturemodule.presenters.ConsentConfirmationPresenter;
import com.visym.collector.dashboardmodule.view.DashboardActivity;
import com.visym.collector.model.ConsentDataQuestionnaire;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.CustomLinearLayoutManager;
import com.visym.collector.utils.Globals;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ConsentConfirmationActivity extends AppCompatActivity implements View.OnClickListener,
        Listeners.PositionListener, Listeners.OnSelectListner<DataModel.ConsentSubjectList>,
        ICaptureModule.IConsentConfirmationView {


    @BindView(R.id.consentRecyclerView)
    RecyclerView consentRecyclerView;
    @BindView(R.id.moreInfo)
    TextView moreInfoText;
    @BindView(R.id.agreeBtn)
    Button agreeBtn;
    @BindView(R.id.disagreeBtn)
    Button disagreeBtn;
    @BindView(R.id.subjectListRecyclerView)
    RecyclerView subjectListRecyclerView;
    @BindView(R.id.recentSubject)
    TextView recentSubject;
    @BindView(R.id.confirmBtn)
    Button confirmBtn;
    @BindView(R.id.subjectEmail)
    TextInputEditText subjectEmail;
    @BindView(R.id.subjectEmailLayout)
    TextInputLayout subjectEmailLayout;
    @BindView(R.id.consentConfirmationGroup)
    Group consentConfirmationGroup;
    @BindView(R.id.consentAgreeDisagreeGroup)
    Group consentAgreeDisagreeGroup;
    @BindView(R.id.userResponseGroup)
    Group userResponseGroup;
    @BindView(R.id.goBackBtn)
    ImageView goBackBtn;
    @BindView(R.id.userConsentResponseBtn)
    Button userConsentResponseBtn;
    @BindView(R.id.endConsentStatmentText)
    TextView endConsentStatmentText;
    @BindView(R.id.consentParentLayout)
    ConstraintLayout consentParentLayout;
    @BindView(R.id.closeBtn)
    ImageView closeButton;
    @BindView(R.id.learn_more_text)
    TextView learnMoreText;


    List<ConsentDataQuestionnaire> consentQuestionsList;
    List<JSONObject> responseCQList;
    List<DataModel.ConsentSubjectList> consentSubjectLists;
    DataModel.ConsentSubjectList consentSubjectList;
    ConsentRecordListAdapter consentRecordListAdapter;
    ConsentSubjectListAdapter consentSubjectListAdapter;
    CustomLinearLayoutManager mLayoutManager;
    JSONArray userQuestionnaireResponseArray, sortedUQResponse;
    int agreeDestinationIdAdapterPosition, disagreeDestinationIdAdapterPosition,
            currentConsentAdapterPosition;
    Boolean isConsentSAgreeDisagreeGroup = false, isconsentConfirmClicked = false, isConsentResponseGroup = false, isDisagreeConsentResponse = false;
    private String consentVersion;
    private ConsentConfirmationPresenter CCPresenter;
    private AppSharedPreference preference;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consent_confirmation);
        ButterKnife.bind(this);
        userQuestionnaireResponseArray = new JSONArray();
        //  getConsentDatas();
        attachPresenter();

        preference = AppSharedPreference.getInstance();
        //subjectListRecyclerView.setVisibility(View.GONE);
        initConsentSubjectObjects();
        consentVersion = "c1";
        bindEvents();
        mContext = this;

        if (preference.readString(Constant.SUBJECT_EMAIL_TEXT) != null) {
            try {
                JSONArray recentSubjects = new JSONArray(preference.readString(Constant.SUBJECT_EMAIL_TEXT));
                if (recentSubjects.length() > 0) {
                    subjectEmail.setText(recentSubjects.getString(recentSubjects.length() - 1));
                } else {
                    subjectEmail.setText(preference.readString(Constant.COLLECTOR_EMAIL));
                }
            } catch (Exception e) {
                Log.e(Globals.TAG, "onCreate: " + e.toString());
            }
        } else {
            subjectEmail.setText(preference.readString(Constant.COLLECTOR_EMAIL));
        }

        if (Globals.isSetupFlow) {
            continueFlowFunction();
            isConsentSAgreeDisagreeGroup = false;
        }


    }

    public void attachPresenter() {
        if (CCPresenter == null) {
            CCPresenter = new ConsentConfirmationPresenter();
        }
        CCPresenter.onViewAttached(this, this);
    }

    @Override
    public void bindEvents() {
        confirmBtn.setOnClickListener(this);
        agreeBtn.setOnClickListener(this);
        disagreeBtn.setOnClickListener(this);
        goBackBtn.setOnClickListener(this);
        userConsentResponseBtn.setOnClickListener(this);
        moreInfoText.setOnClickListener(this);
        learnMoreText.setOnClickListener(this);
        closeButton.setOnClickListener(this);
        learnMoreText.setPaintFlags(learnMoreText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

    }

    @Override
    public void initConsentSubjectObjects() {
        // Stored list in the sharedpref for

        if (preference.readString(Constant.SUBJECT_EMAIL_TEXT) != null) {

            try {
                consentSubjectLists = new ArrayList<>();
                JSONArray recentSubjects = new JSONArray(preference.readString(Constant.SUBJECT_EMAIL_TEXT));
                if (recentSubjects.length() == 0) {
                    recentSubject.setVisibility(View.GONE);
                } else {
                    Log.e(Globals.TAG, "initConsentSubjectObjects: recent Subjeect lenght " + recentSubjects.length());
                    for (int i = recentSubjects.length() - 1; i >= 0; i--) {
                        consentSubjectList = new DataModel.ConsentSubjectList(recentSubjects.getString(i));
                        consentSubjectLists.add(consentSubjectList);
                    }
                    consentSubjectListAdapter = new ConsentSubjectListAdapter(consentSubjectLists, this, this);
                    RecyclerView.LayoutManager manager = new LinearLayoutManager(this);
                    subjectListRecyclerView.setLayoutManager(manager);
                    subjectListRecyclerView.setAdapter(consentSubjectListAdapter);
                }
            } catch (Exception e) {
                Log.e(Globals.TAG, "initConsentSubjectObjects: " + e.toString());
            }
        } else {
            recentSubject.setVisibility(View.GONE);
        }
        // getSubject From the sharedPref, store the recent email input from subject emaillist of email id

    }

    @Override
    public void initConsentDataObjects(JSONArray data) {
        Log.e(Globals.TAG, "initConsentDataObjects: " + data);
        moreInfoText.setVisibility(View.INVISIBLE);
        consentQuestionsList = new ArrayList<>();

        try {
            for (int i = 0; i < data.length(); i++) {
                consentQuestionsList.add(new ConsentDataQuestionnaire(data.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Collections.sort(consentQuestionsList, new AscQuestionId());

        consentRecordListAdapter = new ConsentRecordListAdapter(consentQuestionsList,
                ConsentConfirmationActivity.this, this);
        mLayoutManager = new CustomLinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, true);
        mLayoutManager.setScrollEnabled(false);
        consentRecyclerView.setLayoutManager(mLayoutManager);
        consentRecyclerView.setAdapter(consentRecordListAdapter);
        if (Globals.isShowingLoader()) {
            Globals.dismissLoading();
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.agreeBtn:
                if (agreeDestinationIdAdapterPosition > 0) {
                    agreeBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.darkTextColor));
                    agreeBtn.setTextColor(ContextCompat.getColor(this, R.color.white));

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                consentQuestionsList.get(currentConsentAdapterPosition).setIsAnswered(true);
                                JSONObject responseObject = new JSONObject();
                                responseObject.put("id",
                                        consentQuestionsList.get(currentConsentAdapterPosition).getQuestionId());
                                responseObject.put("q_category", consentQuestionsList.get(currentConsentAdapterPosition).getQ_category());
                                responseObject.put("q_category_response", consentQuestionsList.get(currentConsentAdapterPosition).getQ_category_response());
                                responseObject.put("response", String.valueOf(true));
                                userQuestionnaireResponseArray.put(responseObject);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            consentRecyclerView.scrollToPosition(agreeDestinationIdAdapterPosition);
                            consentRecordListAdapter.notifyDataSetChanged();
                            agreeBtn.setBackgroundResource(R.drawable.custom_black_border);
                            agreeBtn.setTextColor(ContextCompat.getColor(ConsentConfirmationActivity.this, R.color.darkBlack));
                        }
                    }, 200);
                }
                break;
            case R.id.disagreeBtn:
                if (disagreeDestinationIdAdapterPosition >= 0) {
                    disagreeBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.darkTextColor));
                    disagreeBtn.setTextColor(ContextCompat.getColor(this, R.color.white));

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                consentQuestionsList.get(currentConsentAdapterPosition).setIsAnswered(true);
                                JSONObject responseObject = new JSONObject();
                                responseObject.put("id",
                                        consentQuestionsList.get(currentConsentAdapterPosition).getQuestionId());
                                responseObject.put("response", String.valueOf(false));
                                responseObject.put("q_category", consentQuestionsList.get(currentConsentAdapterPosition).getQ_category());
                                responseObject.put("q_category_response", consentQuestionsList.get(currentConsentAdapterPosition).getQ_category_response());
                                userQuestionnaireResponseArray.put(responseObject);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            consentRecyclerView.scrollToPosition(disagreeDestinationIdAdapterPosition);
                            consentRecordListAdapter.notifyDataSetChanged();

                            disagreeBtn.setBackgroundResource(R.drawable.custom_black_border);
                            disagreeBtn.setTextColor(ContextCompat.getColor(ConsentConfirmationActivity.this, R.color.darkBlack));
                        }
                    }, 200);
                }
                break;
            case R.id.confirmBtn:
                isconsentConfirmClicked = true;
                if (!subjectEmail.getText().toString().equalsIgnoreCase("") || subjectEmail.getText().toString().length() != 0) {
                    //    if (subjectEmail.getText().toString().equalsIgnoreCase(preference.readString(Constant.COLLECTOR_EMAIL))) {
                    subjectEmailLayout.setErrorEnabled(false);
                    subjectEmailLayout.setError(null);
                    String consentEmail = subjectEmail.getText().toString();
                    if (TextUtils.isEmpty(consentEmail)) {
                        return;
                    }

                    if (!Patterns.EMAIL_ADDRESS.matcher(consentEmail).matches()) {
                        subjectEmailLayout.setErrorEnabled(true);
                        subjectEmailLayout.setError("Invalid email address");
                        return;
                    }

                    if (preference.readString(Constant.SUBJECT_EMAIL_TEXT) != null) {

                        try {

                            JSONArray recentSubjects = new JSONArray(preference.readString(Constant.SUBJECT_EMAIL_TEXT));
                            if (recentSubjects.length() == 5) {
                                ArrayList<String> list = new ArrayList<String>();
                                int len = recentSubjects.length();
                                for (int i = 0; i < len; i++) {
                                    list.add(recentSubjects.getString(i));
                                }

                                if (list.contains(consentEmail)) {
                                    list.remove(list.indexOf(consentEmail));
                                } else {
                                    list.remove(0);
                                }
                                JSONArray newArray = new JSONArray(list);
                                newArray.put(consentEmail);
                                preference.storeValue(Constant.SUBJECT_EMAIL_TEXT, newArray.toString());
                            } else {
                                if (!recentSubjects.toString().contains(consentEmail)) {
                                    recentSubjects.put(consentEmail);
                                    preference.storeValue(Constant.SUBJECT_EMAIL_TEXT, recentSubjects.toString());
                                } else {
                                    ArrayList<String> list = new ArrayList<String>();
                                    int len = recentSubjects.length();
                                    for (int i = 0; i < len; i++) {
                                        list.add(recentSubjects.getString(i));
                                    }
                                    list.remove(list.indexOf(consentEmail));
                                    JSONArray newArray = new JSONArray(list);
                                    newArray.put(consentEmail);
                                    preference.storeValue(Constant.SUBJECT_EMAIL_TEXT, newArray.toString());
                                }
                            }

                        } catch (Exception e) {
                            Log.e(Globals.TAG, "onClick: json exception " + e.toString());
                        }
                    } else {

                        try {
                            JSONArray recentSubjects = new JSONArray();
                            recentSubjects.put(consentEmail);
                            Log.e(Globals.TAG, "onClick: " + recentSubjects.toString());
                            preference.storeValue(Constant.SUBJECT_EMAIL_TEXT, recentSubjects.toString());
                        } catch (Exception e) {
                            Log.e(Globals.TAG, "onClick: " + e.toString());
                        }
                    }

                    Log.e(Globals.TAG, "onClick: consentEmail  " + consentEmail);
                    Globals.showLoading(this);

                    Globals.getAppSyncClient().query(SubjectByStrSubjectEmailQuery.builder()
                            .subject_email(consentEmail).build())
                            .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                            .enqueue(subjectDev);

                    // }
                    //   else {
                    //       AlertMessage();
                    //    }
                } else {
                    Toast.makeText(this, "Please enter or select your subject's email address", Toast.LENGTH_LONG).show();
                }

                break;
            case R.id.goBackBtn:

                if (isConsentSAgreeDisagreeGroup) {
                    userQuestionnaireResponseArray = new JSONArray();
                    consentConfirmationGroup.setVisibility(View.VISIBLE);
                    recentSubject.setVisibility(View.VISIBLE);
                    learnMoreText.setVisibility(View.VISIBLE);
                    initConsentSubjectObjects();
                    consentAgreeDisagreeGroup.setVisibility(View.GONE);
                    userResponseGroup.setVisibility(View.GONE);
                    isConsentSAgreeDisagreeGroup = false;

                    if (subjectEmail.getText() != null && !subjectEmail.getText().toString().isEmpty()) {
                        subjectEmail.requestFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,
                                InputMethodManager.HIDE_IMPLICIT_ONLY);
                        subjectEmail.setSelection(subjectEmail.getText().length());
                    }
                    if (isconsentConfirmClicked) {
                        try {
                            String consentEmail = subjectEmail.getText().toString();
                            JSONArray recentSubjects = new JSONArray(preference.readString(Constant.SUBJECT_EMAIL_TEXT));
                            // if (recentSubjects.length() == 5) {
                            ArrayList<String> list = new ArrayList<String>();
                            int len = recentSubjects.length();
                            for (int i = 0; i < len; i++) {
                                list.add(recentSubjects.getString(i));
                            }

                            if (list.contains(consentEmail)) {
                                list.remove(list.indexOf(consentEmail));
                            }
                            JSONArray newArray = new JSONArray(list);
                            preference.storeValue(Constant.SUBJECT_EMAIL_TEXT, newArray.toString());
                            //  }
                        } catch (JSONException e) {
                            Log.e(Globals.TAG, "onClick: " + e.toString());
                        }
                    }
                } else {
                    super.onBackPressed();
                }
                break;
            case R.id.userConsentResponseBtn:

                isConsentSAgreeDisagreeGroup = false;
                consentConfirmationGroup.setVisibility(View.VISIBLE);
                consentAgreeDisagreeGroup.setVisibility(View.GONE);
                userResponseGroup.setVisibility(View.GONE);
                if (!isDisagreeConsentResponse) {
                    for (int i = 0; i < consentQuestionsList.size(); i++) {
                        if (!consentQuestionsList.get(i).getIsAnswered()) {
                            try {
                                JSONObject responseObject = new JSONObject();
                                responseObject.put("id", consentQuestionsList.get(i).getQuestionId());
                                responseObject.put("response", String.valueOf(false));
                                responseObject.put("q_category", consentQuestionsList.get(i).getQ_category());
                                responseObject.put("q_category_response", consentQuestionsList.get(i).getQ_category_response());
                                userQuestionnaireResponseArray.put(responseObject);
                            } catch (Exception e) {
                                Log.e(Globals.TAG, "onClick: Json Exception in left out response " + e.toString());
                            }
                        }
                    }

                    responseCQList = new ArrayList<>();
                    try {
                        for (int i = 0; i < userQuestionnaireResponseArray.length(); i++) {
                            responseCQList.add(userQuestionnaireResponseArray.getJSONObject(i));
                        }
                    } catch (Exception e) {
                        Log.e(Globals.TAG, "onClick: exception " + e.toString());
                    }

                    Collections.sort(responseCQList, new Comparator<JSONObject>() {
                        //You can change "Name" with "ID" if you want to sort by ID
                        private static final String KEY_NAME = "id";

                        @Override
                        public int compare(JSONObject a, JSONObject b) {
                            int value = 0;
                            try {
                                if (Integer.parseInt(a.getString("id")) > Integer.parseInt(b.getString("id"))) {
                                    value = 1;
                                } else {
                                    value = -1;
                                }
                            } catch (Exception e) {
                                Log.e(Globals.TAG, "compare: " + e.toString());
                            }
                            return value;
                        }
                    });
                    sortedUQResponse = new JSONArray();
                    for (int i = 0; i < userQuestionnaireResponseArray.length(); i++) {
                        sortedUQResponse.put(responseCQList.get(i));
                        try {
                            Log.e(Globals.TAG, "onClick: sorted " + sortedUQResponse.get(i).toString());
                        } catch (Exception e) {
                            Log.e(Globals.TAG, "onClick: sorted exception " + e.toString());
                        }
                    }


                    // Goto Next screen(consentVideoCaptureActvivity) with the user Questionnire response
                    Intent captureVideoIntent = new Intent(ConsentConfirmationActivity.this,
                            ConsentVideoCaptureActivity.class);

                    if (userQuestionnaireResponseArray != null) {
                        preference.storeValue(Constant.CONSENT_QUESTION_RESPONSE, sortedUQResponse.toString());
                    }
                    if (!consentVersion.isEmpty()) {
                        preference.storeValue(Constant.CONSENT_VERSION, consentVersion);
                    }

                    startActivity(captureVideoIntent);
                    //finish();
                } else {
                    isDisagreeConsentResponse = false;
                    Intent redirectCollectionActivity = new Intent(ConsentConfirmationActivity.this,
                            DashboardActivity.class);
                    redirectCollectionActivity.putExtra("redirectTo", "capture");
                    startActivity(redirectCollectionActivity);
                    finish();
                }
                break;
            case R.id.moreInfo:
                if (consentQuestionsList != null && !consentQuestionsList.isEmpty()){
                    String moreInfo = consentQuestionsList.get(currentConsentAdapterPosition).getMoreInfo();
                    if (!TextUtils.isEmpty(moreInfo)){
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        View view = getLayoutInflater().inflate(R.layout.layout_video_help_icon, null);
                        builder.setView(view);
                        String replacedString = moreInfo.replace("\n", "<br>");
                        TextView descriptionTextView = view.findViewById(R.id.descriptionTextview);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            descriptionTextView.setText(Html.fromHtml(replacedString, Html.FROM_HTML_MODE_LEGACY));
                        } else {
                            descriptionTextView.setText(Html.fromHtml(replacedString));
                        }

                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();

//                        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
//                        Window window = dialog.getWindow();
//                        if (window != null) {
//                            layoutParams.copyFrom(window.getAttributes());
//                            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
//                            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
//                            window.setAttributes(layoutParams);
//                        }
                    }else {
                        Toast.makeText(this, "Content not available", Toast.LENGTH_SHORT).show();
                    }
                }
                break;

            case R.id.learn_more_text:
                displayDialog(getString(R.string.consent_description_text));
                break;
            case R.id.closeBtn:
                displayCloseButtonDialog("Exit consent capture?");
                break;
        }
    }

    private GraphQLCall.Callback<SubjectByStrSubjectEmailQuery.Data> subjectDev = new GraphQLCall.Callback<SubjectByStrSubjectEmailQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<SubjectByStrSubjectEmailQuery.Data> response) {
            runOnUiThread(() -> {
                learnMoreText.setVisibility(View.GONE);
                if (Globals.isShowingLoader()) {
                    Globals.dismissLoading();
                }
                if (response.data() != null) {
                    if (response.data().subjectByStrSubjectEmail() != null &&
                            response.data().subjectByStrSubjectEmail().items().size() > 0) {
                        SubjectByStrSubjectEmailQuery.Item item
                                = response.data().subjectByStrSubjectEmail().items().get(0);
                        String status = item.status();
                        String consentEmail = item.subject_email();
                        preference.storeValue(Constant.SUBJECT_ID_TEXT, item.uuid());
                        if (!TextUtils.isEmpty(status)) {
                            if (status.equals("active")) {
                                try {
                                    JSONArray recentSubjects = new JSONArray(preference.readString(Constant.SUBJECT_EMAIL_TEXT));
                                    ArrayList<String> list = new ArrayList<String>();
                                    int len = recentSubjects.length();
                                    for (int i = 0; i < len; i++) {
                                        list.add(recentSubjects.getString(i));
                                    }
                                    if (list.contains(consentEmail)) {
                                        list.remove(list.indexOf(consentEmail));
                                    }
                                    JSONArray newArray = new JSONArray(list);
                                    newArray.put(consentEmail);
                                    preference.storeValue(Constant.SUBJECT_EMAIL_TEXT, newArray.toString());
                                   /* Intent videoCaptureIntent = new Intent(ConsentConfirmationActivity.this,
                                            VideoCaptureActivity.class);
                                    startActivity(videoCaptureIntent);*/


                                    boolean isPlayTrainingVideo = preference.readBoolean(Constant.IS_PLAY_TRAINING);
                                    if (isPlayTrainingVideo) {
                                        String trainingVideoURL = preference.readString(Constant.TRAINING_VIDEO_URL);
                                        if (TextUtils.isEmpty(trainingVideoURL) || trainingVideoURL.contentEquals("[]")) {
                                            // displayNoTrainingVideoDialog();
                                            Intent goNextIntent = new Intent(ConsentConfirmationActivity.this,
                                                    VideoPlayerActivity.class);
                                            goNextIntent.putExtra("module", "notraining");
                                            goNextIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                            goNextIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(goNextIntent);
                                            //finish();
                                        } else {
                                            Intent goNextIntent = new Intent(ConsentConfirmationActivity.this,
                                                    VideoPlayerActivity.class);
                                            goNextIntent.putExtra("module", "training");
                                            goNextIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                            goNextIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(goNextIntent);
                                            //finish();
                                        }
                                    } else {
                                        Intent setupScreenIntent = new Intent(ConsentConfirmationActivity.this, VideoCaptureActivity.class);
                                        startActivity(setupScreenIntent);
                                    }

                                    //finish();
                                } catch (Exception e) {
                                    Log.e(Globals.TAG, "onResponse: " + e.toString());
                                }
                            } else {
                                displayDialog("This subject is not allowed");
                            }
                        } else {
                            AlertMessage();
                        }
                    } else {
                        AlertMessage();
                        //continueFlowFunction();
                    }
                } else {
                    continueFlowFunction();
                }
            });
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Globals.isShowingLoader()) {
                        Globals.dismissLoading();
                    }
                    Toast.makeText(ConsentConfirmationActivity.this,
                            "Request failed. Please try again later", Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private void displayCloseButtonDialog(String message) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setPositiveButton("OK", (dialog1, which) -> {
            Intent goToCollectionList = new Intent(ConsentConfirmationActivity.this, DashboardActivity.class);
            goToCollectionList.putExtra("redirectTo", "capture");
            startActivity(goToCollectionList);
            finish();
        });
        dialog.show();
    }

    private void displayDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    private void continueFlowFunction() {


        // get the consent with the subject email
        consentConfirmationGroup.setVisibility(View.GONE);
        subjectListRecyclerView.setVisibility(View.GONE);
        recentSubject.setVisibility(View.GONE);
        isConsentSAgreeDisagreeGroup = true;
        Globals.hideKeyboard(ConsentConfirmationActivity.this);
        consentAgreeDisagreeGroup.setVisibility(View.VISIBLE);
        Globals.showLoading(this);
        CCPresenter.getConsentDatas("version");
    }

    @Override
    public void getAdapterPositionFromConsentList(int position) {
        if (position >= 0) {
            if (consentQuestionsList != null) {
                // Log.e("bhushanansnk",   consentQuestionsList.get(position).getAgreeDestinationId());

                currentConsentAdapterPosition = position;

                if (!consentQuestionsList.get(currentConsentAdapterPosition).getAgreeDestinationId().equals("") && !consentQuestionsList.get(currentConsentAdapterPosition).getDisagreeDestinationId().equals("") && Integer.parseInt(consentQuestionsList.get(currentConsentAdapterPosition).getAgreeDestinationId()) > 0 && Integer.parseInt(consentQuestionsList.get(currentConsentAdapterPosition).getAgreeDestinationId()) > 0) {
                    agreeDestinationIdAdapterPosition = Integer.parseInt(consentQuestionsList.get(currentConsentAdapterPosition).getAgreeDestinationId()) - 1;

                    disagreeDestinationIdAdapterPosition = Integer.parseInt(consentQuestionsList.get(currentConsentAdapterPosition).getDisagreeDestinationId()) - 1;

                } else {
                    isConsentResponseGroup = true;
                    Log.e(Globals.TAG, "getAdapterPositionFromConsentList: disagree  " + consentQuestionsList.get(currentConsentAdapterPosition).getDisagreeDestinationId());
                    Log.e(Globals.TAG, "getAdapterPositionFromConsentList: agree  " + consentQuestionsList.get(currentConsentAdapterPosition).getAgreeDestinationId());
                    if (consentQuestionsList.get(currentConsentAdapterPosition).getDisagreeDestinationId() != null && consentQuestionsList.get(currentConsentAdapterPosition).getDisagreeDestinationId() != "") {
                        isDisagreeConsentResponse = true;
                    }
                    hideTheConsentView(consentQuestionsList.get(currentConsentAdapterPosition).getShortDescription());
                }

            }
        }
    }

    @Override
    public void onBackPressed() {
        if (isConsentSAgreeDisagreeGroup) {
            consentConfirmationGroup.setVisibility(View.VISIBLE);
            consentAgreeDisagreeGroup.setVisibility(View.GONE);
            userResponseGroup.setVisibility(View.GONE);
            isConsentSAgreeDisagreeGroup = false;
            initConsentSubjectObjects();
            if (subjectEmail.getText() != null && !subjectEmail.getText().toString().isEmpty()) {
                subjectEmail.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
                subjectEmail.setSelection(subjectEmail.getText().length());
            }
        } else {
            super.onBackPressed();
        }

    }

    @Override
    public void onCollectionSelection(DataModel.ConsentSubjectList collectionsData, int position) {

        subjectEmail.setText(collectionsData.getSubjectEmail());
        subjectEmail.requestFocus();
        // InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        //imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        InputMethodManager inputMethodManager = (InputMethodManager) mContext.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
        subjectEmail.setSelection(collectionsData.getSubjectEmail().length());
    }

    @Override
    public void hideTheConsentView(String endStatmentText) {
        consentConfirmationGroup.setVisibility(View.INVISIBLE);
        consentAgreeDisagreeGroup.setVisibility(View.INVISIBLE);
        consentConfirmationGroup.updatePreLayout(consentParentLayout);
        consentAgreeDisagreeGroup.updatePreLayout(consentParentLayout);
        userResponseGroup.setVisibility(View.VISIBLE);
        endConsentStatmentText.setText(endStatmentText);
    }

    @Override
    public void displayRequestFailed() {

    }

    class AscQuestionId implements Comparator<ConsentDataQuestionnaire> {

        @Override
        public int compare(ConsentDataQuestionnaire e1, ConsentDataQuestionnaire e2) {
            Log.e(Globals.TAG, "compare: e1 " + e1.getQuestionId() + " e2  " + e2.getQuestionId());
            if (Integer.parseInt(e1.getQuestionId()) > Integer.parseInt(e2.getQuestionId())) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    public void AlertMessage() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this, R.style.AlertDialogStyle);
        alertDialogBuilder.setTitle("Alert");
        alertDialogBuilder.setMessage("New subject email entered - Would you like to consent?").setCancelable(false)
                .setPositiveButton("Yes",

                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                subjectEmailLayout.setErrorEnabled(false);
                                subjectEmailLayout.setError(null);
                                String consentEmail = subjectEmail.getText().toString();
                                if (TextUtils.isEmpty(consentEmail)) {
                                    return;
                                }


                                if (!Patterns.EMAIL_ADDRESS.matcher(consentEmail).matches()) {
                                    subjectEmailLayout.setErrorEnabled(true);
                                    subjectEmailLayout.setError("Invalid email address");
                                    return;
                                }


                                if (preference.readString(Constant.SUBJECT_EMAIL_TEXT) != null) {
                                    try {

                                        JSONArray recentSubjects = new JSONArray(preference.readString(Constant.SUBJECT_EMAIL_TEXT));
                                        ArrayList<String> list = new ArrayList<String>();
                                        int len = recentSubjects.length();
                                        for (int i = 0; i < len; i++) {
                                            list.add(recentSubjects.getString(i));
                                        }
                                        if (recentSubjects.length() == 5) {
                                            if (list.contains(consentEmail)) {
                                                list.remove(list.indexOf(consentEmail));
                                            } else {
                                                list.remove(0);
                                            }
                                            JSONArray newArray = new JSONArray(list);
                                            newArray.put(consentEmail);
                                            preference.storeValue(Constant.SUBJECT_EMAIL_TEXT, newArray.toString());
                                        } else {

                                            if (list.contains(consentEmail)) {
                                                list.remove(list.indexOf(consentEmail));
                                            } else {
                                                list.remove(0);
                                            }
                                            JSONArray newArray1 = new JSONArray(list);
                                            newArray1.put(consentEmail);
                                            //recentSubjects.put(consentEmail);
                                            preference.storeValue(Constant.SUBJECT_EMAIL_TEXT, newArray1.toString());
                                        }

                                    } catch (Exception e) {
                                        Log.e(Globals.TAG, "onClick: json exception " + e.toString());
                                    }
                                } else {
                                    try {
                                        JSONArray recentSubjects = new JSONArray();
                                        recentSubjects.put(consentEmail);
                                        Log.e(Globals.TAG, "onClick: " + recentSubjects.toString());
                                        preference.storeValue(Constant.SUBJECT_EMAIL_TEXT, recentSubjects.toString());
                                    } catch (Exception e) {
                                        Log.e(Globals.TAG, "onClick: " + e.toString());
                                    }
                                }


                                Log.e(Globals.TAG, "onClick: consentEmail  " + consentEmail);

                                // Globals.showLoading(this);
                                /*Globals.getAppSyncClient().query(SubjectByStrSubjectEmailQuery.builder()
                                Globals.showLoading(ConsentConfirmationActivity.this);
                                Globals.getAppSyncClient().query(SubjectByStrSubjectEmailQuery.builder()

                                /*Globals.getAppSyncClient().query(SubjectByStrSubjectEmailQuery.builder()
                                /*Globals.getAppSyncClient().query(SubjectByStrSubjectEmailQuery.builder()
                                        .subject_email(consentEmail).build())
                                        .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                                        .enqueue(subjectDev);*/
                                continueFlowFunction();

                            }
                        })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            if (isconsentConfirmClicked) {

                                String consentEmail = subjectEmail.getText().toString();
                                JSONArray recentSubjects = new JSONArray(preference.readString(Constant.SUBJECT_EMAIL_TEXT));
                                // if (recentSubjects.length() == 5) {
                                ArrayList<String> list = new ArrayList<String>();
                                int len = recentSubjects.length();
                                for (int i = 0; i < len; i++) {
                                    list.add(recentSubjects.getString(i));
                                }

                                if (list.contains(consentEmail)) {
                                    list.remove(list.indexOf(consentEmail));
                                }
                                JSONArray newArray = new JSONArray(list);
                                preference.storeValue(Constant.SUBJECT_EMAIL_TEXT, newArray.toString());
                                isconsentConfirmClicked = false;
                                //  }
                            }
                        } catch (JSONException e) {
                            Log.e(Globals.TAG, "onClick: " + e.toString());
                        }
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

}
