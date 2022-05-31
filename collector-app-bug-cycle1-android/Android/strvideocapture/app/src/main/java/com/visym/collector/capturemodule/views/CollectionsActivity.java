package com.visym.collector.capturemodule.views;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.visym.collector.R;
import com.visym.collector.capturemodule.ICaptureModule.ICollectionsView;
import com.visym.collector.capturemodule.adapters.CollectionsListAdapter;
import com.visym.collector.capturemodule.interfaces.Listeners;
import com.visym.collector.capturemodule.presenters.CollectionsPresenter;
import com.visym.collector.model.CollectionModel;
import com.visym.collector.network.ConnectivityReceiver;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.Globals;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;

public class CollectionsActivity extends AppCompatActivity implements View.OnClickListener,
    SearchView.OnQueryTextListener, Listeners.OnSelectListner<CollectionModel>,
    ConnectivityReceiver.ConnectivityReceiverListener, ICollectionsView {

  @BindView(R.id.searchImageBtn)
  SearchView searchBtn;
  @BindView(R.id.cillectionh)
  TextView cillectionh;

  @BindView(R.id.collectionsListRecyclerView)
  RecyclerView collectionsListRecyclerView;
  private CollectionsListAdapter collectionsListAdapter;
  private List<CollectionModel> collectionsList;
  private String projectId;
  private CollectionsPresenter CPresenter;
  private AppSharedPreference preference;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.d("JEBYRNE", String.format("CollectionsActivity.onCreate"));
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_collections);
    ButterKnife.bind(this);
    attachPresenter();
    bindEvents();

    preference = AppSharedPreference.getInstance();
    projectId = preference.readString(Constant.PROJECT_ID_KEY);
    CPresenter.getCollectionsForProjectFromServer(projectId);

  }


  @Override
  public void onClick(View v) {
    Log.d("JEBYRNE", String.format("CollectionsActivity.onClick"));

    switch (v.getId()) {
      case R.id.searchImageBtn:
        cillectionh.setVisibility(View.GONE);
        break;
    }
  }

  @Override
  public boolean onQueryTextSubmit(String query) {
    Log.d("JEBYRNE", String.format("CollectionsActivity.onQueryTextSubmit"));

    if (collectionsListAdapter != null) {
      collectionsListAdapter.getFilter().filter(query);
    }
    return false;
  }

  @Override
  public boolean onQueryTextChange(String newText) {
    Log.d("JEBYRNE", String.format("CollectionsActivity.onQueryTextChange"));

    if (collectionsListAdapter != null) {
      Filter filter = collectionsListAdapter.getFilter();
      if (filter != null) {
        filter.filter(newText);
      }
    }
    return false;
  }


  @Override
  public void onCollectionSelection(CollectionModel collectionsData, int position) {
    Log.d("JEBYRNE", String.format("CollectionsActivity.onCollectionSelection"));

    preference.storeValue(Constant.COLLECTION_NAME_KEY, collectionsData.getCollectionName());

    String label = collectionsData.getDefaultObject();
    preference.storeValue(AppSharedPreference.FRAME_OBJECT_LABEL, label);

    Intent redirectIntent = new Intent(CollectionsActivity.this,
            CollectionDetailsActivity.class);
    redirectIntent.putExtra("dataBundle", collectionsData);
    startActivity(redirectIntent);
  }

  @Override
  public void onNetworkConnectionChanged(boolean isConnected) {
    Log.d("JEBYRNE", String.format("CollectionsActivity.onNetworkConnectionChanged"));

    if (!isConnected) {
      Globals.showSnackBar(getResources().getString(R.string.noInternet), this,
          Snackbar.LENGTH_INDEFINITE);
    } else {
      Globals.showSnackBar(getResources().getString(R.string.internet), this, Snackbar.LENGTH_LONG);
    }
  }

  public void attachPresenter() {
    Log.d("JEBYRNE", String.format("CollectionsActivity.attachPresenter"));

    if (CPresenter == null) {
      CPresenter = new CollectionsPresenter();
    }
    CPresenter.onViewAttached(this, this);
  }

  @Override
  public void bindEvents() {
    Log.d("JEBYRNE", String.format("CollectionsActivity.bindEvents"));

    searchBtn.setOnSearchClickListener(this);
    searchBtn.setOnQueryTextListener(this);
    searchBtn.setOnCloseListener(new SearchView.OnCloseListener() {
      @Override
      public boolean onClose() {
        searchBtn.setIconifiedByDefault(true);
        cillectionh.setVisibility(View.VISIBLE);

        return false;
      }
    });
  }

  @Override
  public void initializeAdapterWithDatas(JSONArray dataArray) {
    Log.d("JEBYRNE", String.format("CollectionsActivity.initializeAdapterWithDatas"));

    collectionsList = new ArrayList<>();
  }

  @Override
  public void getCollectionProjects(JSONArray dataArray) {
    Log.d("JEBYRNE", String.format("CollectionsActivity.getCollectionProjects"));


  }

}
