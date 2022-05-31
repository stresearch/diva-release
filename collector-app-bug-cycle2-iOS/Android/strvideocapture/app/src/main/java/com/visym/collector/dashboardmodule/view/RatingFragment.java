package com.visym.collector.dashboardmodule.view;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.Group;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.visym.collector.R;
import com.visym.collector.capturemodule.interfaces.Listeners.OnSelectListner;
import com.visym.collector.capturemodule.views.CollectionsActivity;
import com.visym.collector.capturemodule.views.VideoPlayerActivity;
import com.visym.collector.dashboardmodule.IDashboardModule.IRatingFragmentPresenter;
import com.visym.collector.dashboardmodule.IDashboardModule.IRatingFragmentView;
import com.visym.collector.dashboardmodule.OnLoadMoreListener;
import com.visym.collector.dashboardmodule.Projects;
import com.visym.collector.dashboardmodule.adapters.GalleryVideoAdapter;
import com.visym.collector.dashboardmodule.presenter.RatingFragmentPresenter;
import com.visym.collector.model.GalleryVideoList;
import com.visym.collector.usermodule.view.UserProfileActivity;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.Globals;
import com.visym.collector.utils.MyDrawerLayout;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class RatingFragment extends Fragment implements View.OnClickListener, IRatingFragmentView,
        OnSelectListner<GalleryVideoList>, NavigationView.OnNavigationItemSelectedListener {

    protected Handler handler;
    @BindView(R.id.galleryVideoRecyclerView)
    RecyclerView galleryVideoRecyclerView;
    @BindView(R.id.menuIcon)
    ImageView menuIcon;
    @BindView(R.id.menuDrawerLayout)
    MyDrawerLayout menuDrawerLayout;
    @BindView(R.id.navigationView)
    NavigationView navigationView;
    GalleryVideoAdapter galleryVideoAdapter;
    @BindView(R.id.footer_item_1)
    TextView footer_item_1;
    @BindView(R.id.termsOfUse)
    TextView termsOfUse;
    @BindView(R.id.swiperefresh)
    SwipeRefreshLayout swiperefresh;
    @BindView(R.id.emptyVideoScreenGroup)
    Group emptyVideoScreenGroup;
    List<GalleryVideoList> galleryVideoList = new ArrayList<>();
    IRatingFragmentPresenter IRPresenter;
    private ActionBarDrawerToggle drawerToggle;
    private AppSharedPreference preference;


    public RatingFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_ratings, container, false);
        ButterKnife.bind(this, v);
        attachPresenter();
        bindEvents();
        handler = new Handler();
        String versionName = "";
        int versionCode = -1;
        try {
            PackageInfo packageInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            versionName = packageInfo.versionName;
            versionCode = packageInfo.versionCode;
            footer_item_1.setText("Version " + versionName + " | ");//+ " | " + "Terms of Use");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (menuDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    menuDrawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    if (getActivity() != null)
                        getActivity().finish();
                }
            }
        };

        swiperefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                IRPresenter.getUserRatingsVideo(true);
            }
        });
        preference = AppSharedPreference.getInstance();
        requireActivity().getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
        menuDrawerLayout.addDrawerListener(drawerToggle);
        IRPresenter.getUserRatingsVideo(false);
        View headerView = navigationView.getHeaderView(0);
        AppCompatTextView userEmail = headerView.findViewById(R.id.userEmail);
        AppCompatTextView userName = headerView.findViewById(R.id.userName);
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
        return v;
    }

    private void attachPresenter() {
        if (IRPresenter == null) {
            IRPresenter = new RatingFragmentPresenter();
        }
        IRPresenter.onViewAttached(this, getActivity());
    }

    @Override
    public void handleDataAndInitializeAdapter(JSONArray videoData) {
        galleryVideoList.clear();
        GalleryVideoList videoList;
        if (videoData.length() > 0) {
            if (emptyVideoScreenGroup.getVisibility() == View.VISIBLE) {
                emptyVideoScreenGroup.setVisibility(View.GONE);
            }
            try {
                for (int i = 0; i < videoData.length(); i++) {
                    videoList = new GalleryVideoList(videoData.getJSONObject(i));
                    galleryVideoList.add(videoList);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
            galleryVideoRecyclerView.setLayoutManager(layoutManager);
            galleryVideoAdapter = new GalleryVideoAdapter(galleryVideoList, this, getActivity(), galleryVideoRecyclerView);
            galleryVideoRecyclerView.setAdapter(galleryVideoAdapter);
            galleryVideoAdapter.setOnLoadMoreListener(new OnLoadMoreListener() {
                @Override
                public void onLoadMore() {
                    IRPresenter.getLoadMoreDatas(false);
                }
            });
            if (Globals.isShowingLoader()) {
                Globals.dismissLoading();
            }
            if (Globals.isShowingLoader2()) {
                Globals.dismissLoading2();
            }
        } else {
            if (Globals.isShowingLoader()) {
                Globals.dismissLoading();
            }
            if (Globals.isShowingLoader2()) {
                Globals.dismissLoading2();
            }
            emptyVideoScreenGroup.setVisibility(View.VISIBLE);
        }
        if (swiperefresh.isRefreshing()) {
            swiperefresh.setRefreshing(false);
        }
    }

    @Override
    public void handleLoadMoreDataAndInitializeAdapter(JSONArray videoData) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                GalleryVideoList userVideoList;
                for (int i = 0; i < videoData.length(); i++) {
                    try {
                        userVideoList = new GalleryVideoList(videoData.getJSONObject(i));
                        galleryVideoList.add(userVideoList);
                       } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                //add items one by one

                galleryVideoAdapter.setLoaded();
                galleryVideoAdapter.notifyDataSetChanged();
                if (Globals.isShowingLoader()) {
                    Globals.dismissLoading();
                }
                if (Globals.isShowingLoader2()) {
                    Globals.dismissLoading2();

                }
            }
        }, 2000);
    }

    @Override
    public void bindEvents() {
        menuIcon.setOnClickListener(this);
        navigationView.setNavigationItemSelectedListener(this);
        termsOfUse.setOnClickListener(this);
    }

    @Override
    public void handleError() {
        if(Globals.isShowingLoader()){
            Globals.dismissLoading();
        }
        Toast.makeText(getActivity(), getResources().getString(R.string.unknown_error_message), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCollectionSelection(GalleryVideoList collectionsData, int position) {
        if (getActivity() == null) {
            return;
        }
        Intent intent = new Intent(getActivity(), VideoPlayerActivity.class);
        intent.putExtra("module", "ratings");
        intent.putExtra("jsonURL", collectionsData.getAnnotation_file_path());
        intent.putExtra("videoURL", collectionsData.getRaw_video_file_path());
        intent.putExtra("videoId", collectionsData.getVideo_id());
        intent.putExtra("instanceData", collectionsData.getInstance_ids());
        getActivity().startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.menuIcon:
                if (menuDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    menuDrawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    menuDrawerLayout.openDrawer(GravityCompat.START);
                }
                break;
            case R.id.captureActivityBtn:
                if (preference.readString(Constant.PROGRAM_ID_KEY) != null) {
                    Intent collectionIntent = new Intent(getActivity(), CollectionsActivity.class);
                    startActivity(collectionIntent);
                } else {
                    ((DashboardActivity) getActivity()).redirectToFragment("capture");
                }
                break;
            case R.id.termsOfUse:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://visym.com/legal.html"));
                startActivity(browserIntent);
                break;

        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        menuItem.setChecked(true);

        menuDrawerLayout.closeDrawers();

        int id = menuItem.getItemId();
        switch (id) {
            case R.id.projects:
                if (getActivity() != null) {
                    Intent projectIntent = new Intent(getActivity(), Projects.class);
                    getActivity().startActivityForResult(projectIntent,
                         DashboardActivity.RATING_REQUEST_CODE);
                }
                break;

            case R.id.profile:
                Intent profileIntent = new Intent(getActivity(), UserProfileActivity.class);
                getActivity().startActivity(profileIntent);
                break;

            case R.id.faq:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://visym.com/faq.html"));
                startActivity(browserIntent);
                break;

            case R.id.signOut:
                ((DashboardActivity) getActivity()).signOutFunctionality();
                break;

        }
        return true;
    }

    @Override
    public void handleDataNull() {
        if (Globals.isShowingLoader()) {
            Globals.dismissLoading();
        }
        Globals.showSnackBar("No Data Available!", getActivity(), Snackbar.LENGTH_LONG);
    }

}
