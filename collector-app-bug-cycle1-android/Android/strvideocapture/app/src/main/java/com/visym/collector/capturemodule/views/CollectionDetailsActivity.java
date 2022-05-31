package com.visym.collector.capturemodule.views;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.visym.collector.R;
import com.visym.collector.capturemodule.adapters.CollectionActivitiesAdapter;
import com.visym.collector.dashboardmodule.view.DashboardActivity;
import com.visym.collector.model.CollectionActivitiesModel;
import com.visym.collector.model.CollectionModel;
import com.visym.collector.usermodule.view.FrontScreenActivity;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.FileUtil;
import com.visym.collector.utils.Globals;

import org.json.JSONArray;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CollectionDetailsActivity extends AppCompatActivity implements View.OnClickListener {

    @BindView(R.id.nextBtn)
    Button nextBtn;
    @BindView(R.id.descriptionTextview)
    TextView descriptionTextview;
    @BindView(R.id.titleText)
    TextView titleText;
    @BindView(R.id.goBackBtn)
    ImageView goBackBtn;
    String projectId, collectionName, collectionId,collectionDescription, id, trainingVideoURL,consentOverlayText;
    boolean isConsentRequired = false, isPlayTrainingVideo = false;
    CollectionModel collectionModelObject;
    Context mContext;
    private AppSharedPreference preference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("JEBYRNE","CollectionDetailsActivity.onCreate");

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection_details);
        ButterKnife.bind(this);
        nextBtn.setOnClickListener(this);
        goBackBtn.setOnClickListener(this);

        preference = AppSharedPreference.getInstance();
        projectId = preference.readString(Constant.PROJECT_ID_KEY);
        mContext = this;
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("dataBundle")) {
            collectionModelObject = getIntent().getParcelableExtra("dataBundle");
            collectionId = collectionModelObject.getCollectionId();
            collectionName = collectionModelObject.getCollectionName();
            collectionDescription = collectionModelObject.getCollectionDescription();
            id = collectionModelObject.getId();
            consentOverlayText = collectionModelObject.getConsentOverLayText();
            preference.storeValue(Constant.CONSENT_OVERLAY_TEXT,consentOverlayText);
            isConsentRequired = collectionModelObject.isConsentRequired();
            isPlayTrainingVideo = collectionModelObject.isPlayTrainingVideo();
            preference.storeValue(Constant.IS_CONSENT_REQUIRED,isConsentRequired);
            preference.storeValue(Constant.TRAINING_VIDEO_URL,collectionModelObject.getTrainingVideoURL());
            trainingVideoURL = collectionModelObject.getTrainingVideoURL();
            titleText.setText(collectionName);

            String description = collectionModelObject.getCollectionDescription();
            if (!TextUtils.isEmpty(description)) {
                String replacedString = description.replace("\n", "<br>");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    descriptionTextview.setText(Html.fromHtml(replacedString, Html.FROM_HTML_MODE_LEGACY));
                } else {
                    descriptionTextview.setText(Html.fromHtml(replacedString));
                }
            }
        }
        else {
            logout();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.goBackBtn:
                Log.d("JEBYRNE","CollectionDetailsActivity.onClick: BACK");
                onBackPressed();
                break;
            case R.id.nextBtn:
                preference.storeValue(Constant.COLLECTION_KEY, new Gson().toJson(collectionModelObject));
                preference.storeValue(Constant.COLLECTION_ID_KEY, collectionId);
                preference.storeValue(Constant.CONSENT_OVERLAY_TEXT,consentOverlayText);
                preference.storeValue(Constant.COLLECTION_DESCRIPTION,collectionDescription);
                preference.storeValue(Constant.SUBJECT_ID_TEXT, preference.readString(Constant.COLLECTOR_ID_KEY));  // JEBYRNE: reset me
                Globals.isSetupFlow = false;

                if(isConsentRequired){
                    String trainingVideoURL = collectionModelObject.getTrainingVideoURL();
                    if (TextUtils.isEmpty(trainingVideoURL) || trainingVideoURL.contentEquals("[]")){
                        preference.storeValue(Constant.IS_PLAY_TRAINING, false);
                    }else {
                        preference.storeValue(Constant.IS_PLAY_TRAINING, isPlayTrainingVideo);
                    }
                    Intent goToConsent = new Intent(CollectionDetailsActivity.this,
                            ConsentConfirmationActivity.class);
                    goToConsent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(goToConsent);
                }else if(isPlayTrainingVideo){
                    preference.storeValue(Constant.SUBJECT_ID_TEXT, "");
                    String trainingVideoURL = collectionModelObject.getTrainingVideoURL();
                    if (TextUtils.isEmpty(trainingVideoURL) || trainingVideoURL.contentEquals("[]")){
                        preference.storeValue(Constant.IS_PLAY_TRAINING, false);
                        displayDialog();
                    }else {
                        preference.storeValue(Constant.IS_PLAY_TRAINING, isPlayTrainingVideo);
                        Intent goNextIntent = new Intent(CollectionDetailsActivity.this,
                                VideoPlayerActivity.class);
                        goNextIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        goNextIntent.putExtra("module","training");
                        startActivity(goNextIntent);
                        //finish();
                    }
                }else {
                    String trainingVideoURL = collectionModelObject.getTrainingVideoURL();
                    if (TextUtils.isEmpty(trainingVideoURL) || trainingVideoURL.contentEquals("[]")){
                        preference.storeValue(Constant.IS_PLAY_TRAINING, false);
                    }else {
                        preference.storeValue(Constant.IS_PLAY_TRAINING, isPlayTrainingVideo);
                    }
                    Intent goToConsent = new Intent(CollectionDetailsActivity.this,
                            VideoCaptureActivity.class);
                    goToConsent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(goToConsent);
                }
                break;

        }
    }

    private void displayDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setCancelable(false)
                .setMessage("Training video for this collection is in development")
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent goNextIntent = new Intent(this,
                            VideoCaptureActivity.class);
                    goNextIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(goNextIntent);
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // JEBYRNE: common function to return to entry screen on error
    private void logout() {

        AlertDialog.Builder builder = new AlertDialog.Builder(CollectionDetailsActivity.this);
        builder.setMessage("Network Error!  Restarting due to poor network connection...");
        builder.setCancelable(false);
        builder.setPositiveButton("OK", (dialog1, which) -> {
            Intent loginIntent = new Intent(this, FrontScreenActivity.class);
            this.startActivity(loginIntent);
            finish();
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}
