package com.visym.collector.dashboardmodule.view;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.visym.collector.R;
import com.visym.collector.capturemodule.ICaptureModule;
import com.visym.collector.capturemodule.adapters.CollectionsListAdapter;
import com.visym.collector.capturemodule.interfaces.Listeners;
import com.visym.collector.capturemodule.presenters.CollectionsPresenter;
import com.visym.collector.capturemodule.views.CollectionDetailsActivity;
import com.visym.collector.dashboardmodule.Projects;
import com.visym.collector.model.CollectionModel;
import com.visym.collector.model.ProjectDataList;
import com.visym.collector.network.ConnectivityReceiver;
import com.visym.collector.usermodule.view.UserProfileActivity;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.Globals;
import com.visym.collector.utils.MyDrawerLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ProjectCollections extends Fragment implements View.OnClickListener,
        SearchView.OnQueryTextListener, Listeners.OnSelectListner<CollectionModel>,
        ConnectivityReceiver.ConnectivityReceiverListener, ICaptureModule.ICollectionsView, NavigationView.OnNavigationItemSelectedListener {

    @BindView(R.id.searchImageBtn)
    SearchView searchBtn;
    @BindView(R.id.cillectionh)
    TextView cillectionh;
    @BindView(R.id.menuDrawerLayout)
    MyDrawerLayout menuDrawerLayout;
    @BindView(R.id.menuIcon)
    ImageView menuIcon;
    @BindView(R.id.navigationView)
    NavigationView navigationView;
    @BindView(R.id.collectionsListRecyclerView)
    RecyclerView collectionsListRecyclerView;
    @BindView(R.id.footer_item_1)
    TextView footer_item_1;
    @BindView(R.id.termsOfUse)
    TextView termsOfUse;
    private CollectionsListAdapter collectionsListAdapter;
    private List<CollectionModel> collectionsList;
    private String projectId;
    private CollectionsPresenter CPresenter;
    private AppSharedPreference preference;
    private Context mContext;
    private boolean isOnCreateView = false;
    private ActionBarDrawerToggle drawerToggle;

    public ProjectCollections() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mContext = context;
    }

    public static ProjectCollections newInstance() {
        ProjectCollections fragment = new ProjectCollections();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.activity_collections, container, false);
        ButterKnife.bind(this, fragmentView);

        String versionName = "";
        int versionCode = -1;
        try {
            PackageInfo packageInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            versionName = packageInfo.versionName;
            versionCode = packageInfo.versionCode;
            footer_item_1.setText("Version " + versionName + " | ");// + "Terms of Use");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        menuDrawerLayout.addDrawerListener(drawerToggle);
        preference = AppSharedPreference.getInstance();

        View headerView = navigationView.getHeaderView(0);
        AppCompatTextView userEmail = (AppCompatTextView) headerView.findViewById(R.id.userEmail);
        AppCompatTextView userName = (AppCompatTextView) headerView.findViewById(R.id.userName);
        if (preference.readString(Constant.COLLECTOR_EMAIL) != null)
            userEmail.setText(preference.readString(Constant.COLLECTOR_EMAIL));
        if (preference.readString(Constant.COLLECTOR_FNAME) != null) {
            if (preference.readString(Constant.COLLECTOR_LNAME) != null) {
                String username = preference.readString(Constant.COLLECTOR_FNAME) + " " + preference.readString(Constant.COLLECTOR_LNAME);
                userName.setText(username);
            } else {
                userName.setText(preference.readString(Constant.COLLECTOR_FNAME));
            }

        }

        OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (menuDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    menuDrawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    if (getActivity() != null)
                        ((DashboardActivity) getActivity()).finish();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
        attachPresenter();
        bindEvents();
        isOnCreateView = true;
        preference = AppSharedPreference.getInstance();
        projectId = preference.readString(Constant.PROJECT_ID_KEY);
        if (projectId == null) {
            Globals.showLoading(getActivity());
            CPresenter.getProjectList();
        } else {
            CPresenter.getCollectionsForProjectFromServer(projectId);
        }
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!isOnCreateView && projectId != null && preference.readString(Constant.PROJECT_ID_KEY) != null
                && !projectId.equalsIgnoreCase(preference.readString(Constant.PROJECT_ID_KEY))) {
            projectId = preference.readString(Constant.PROJECT_ID_KEY);
            CPresenter.getCollectionsForProjectFromServer(projectId);

        }
        isOnCreateView = false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.video_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.searchImageBtn:
                cillectionh.setVisibility(View.GONE);
                break;
            case R.id.menuIcon:
                if (menuDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    menuDrawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    menuDrawerLayout.openDrawer(GravityCompat.START);
                }
                break;

            case R.id.termsOfUse:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://visym.com/legal.html"));
                startActivity(browserIntent);
                break;

        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (collectionsListAdapter != null) {
            collectionsListAdapter.getFilter().filter(query);
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
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
        preference.storeValue(Constant.COLLECTION_NAME_KEY, collectionsData.getCollectionName());
        String label = collectionsData.getDefaultObject();
        preference.storeValue(AppSharedPreference.FRAME_OBJECT_LABEL, label);
        Intent redirectIntent = new Intent(mContext, CollectionDetailsActivity.class);
        redirectIntent.putExtra("dataBundle", collectionsData);
        startActivity(redirectIntent);
    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        if (!isConnected) {
            Globals.showSnackBar(getResources().getString(R.string.noInternet), mContext,
                    Snackbar.LENGTH_INDEFINITE);
        } else {
            Globals.showSnackBar(getResources().getString(R.string.internet), mContext, Snackbar.LENGTH_LONG);
        }
    }

    public void attachPresenter() {
        if (CPresenter == null) {
            CPresenter = new CollectionsPresenter();
        }
        CPresenter.onViewAttached(this, mContext);
    }


    @Override
    public void bindEvents() {
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
        menuIcon.setOnClickListener(this);
        navigationView.setNavigationItemSelectedListener(this);
        termsOfUse.setOnClickListener(this);

    }


    @Override
    public void initializeAdapterWithDatas(JSONArray dataArray) {
        CollectionModel collectionModel;
        collectionsList = new ArrayList<>();
        for (int i = 0; i < dataArray.length(); i++) {
            try {
                collectionModel = new CollectionModel(dataArray.getJSONObject(i));
                collectionsList.add(collectionModel);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        initAdapter();
    }

    @Override
    public void getCollectionProjects(JSONArray object) {
        ArrayList<ProjectDataList> pDataList = new ArrayList<>();
        ProjectDataList projectDataList = null;


        try {
            JSONObject projectObject = new JSONObject();
            projectObject.put("id",object.getJSONObject(0).getString("program_name"));
            projectObject.put("project_id",object.getJSONObject(0).getString("project_id"));
            projectObject.put("name",object.getJSONObject(0).getString("project_name"));

            projectDataList = new ProjectDataList(projectObject);
            pDataList.add(projectDataList);
            String project_id = projectDataList.getId() + "_" + projectDataList.getProjectName();
            projectId = projectDataList.getProjectId();
            AppSharedPreference.getInstance().storeValue(Constant.PROGRAM_ID_KEY, projectDataList.getId());
            AppSharedPreference.getInstance().storeValue(Constant.PROJECT_ID_KEY, projectDataList.getProjectId());
            AppSharedPreference.getInstance().storeValue("id", project_id);
            AppSharedPreference.getInstance().storeValue(Constant.PROJECT_NAME_KEY, projectDataList.getProjectName());
            if (pDataList.size() > 0) {
                CPresenter.getCollectionsForProjectFromServer(projectId);
            } else {
                Globals.dismissLoading();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initAdapter() {
        collectionsListAdapter = new CollectionsListAdapter(collectionsList, this, mContext);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(mContext.getApplicationContext());
        collectionsListRecyclerView.setLayoutManager(mLayoutManager);
        collectionsListRecyclerView.setNestedScrollingEnabled(false);
        collectionsListRecyclerView.setAdapter(collectionsListAdapter);
        Globals.dismissLoading();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        menuItem.setChecked(true);
        menuDrawerLayout.closeDrawers();
        int id = menuItem.getItemId();
        switch (id) {
            case R.id.projects:
                Intent projectIntent = new Intent(getActivity(), Projects.class);
                getActivity().startActivity(projectIntent);
                break;

            case R.id.profile:
                Intent profileIntent = new Intent(getActivity(), UserProfileActivity.class);
                getActivity().startActivity(profileIntent);
                break;

            case R.id.faq:
                Toast.makeText(getActivity(), "Faq", Toast.LENGTH_SHORT).show();
                break;

            case R.id.signOut:
                ((DashboardActivity) getActivity()).signOutFunctionality();
                break;

        }
        return true;
    }
}
