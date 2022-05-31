package com.visym.collector.dashboardmodule.view;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.visym.collector.BaseActivity;
import com.visym.collector.R;
import com.visym.collector.capturemodule.interfaces.Listeners;
import com.visym.collector.capturemodule.views.VideoEditorActivity;
import com.visym.collector.dashboardmodule.IDashboardModule.ICollectionFragmentPresenter;
import com.visym.collector.dashboardmodule.IDashboardModule.ICollectionFragmentView;
import com.visym.collector.dashboardmodule.OnLoadMoreListener;
import com.visym.collector.dashboardmodule.Projects;
import com.visym.collector.dashboardmodule.adapters.MyVideoAdapter;
import com.visym.collector.dashboardmodule.presenter.CollectionFragmentPresenter;
import com.visym.collector.model.UserVideoList;
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


public class CollectionFragment extends Fragment implements ICollectionFragmentView,
        Listeners.OnSelectListner<UserVideoList>, Listeners.OnEditOptionSelectListner, View.OnClickListener, NavigationView.OnNavigationItemSelectedListener {

    protected Handler handler;
    @BindView(R.id.captureActivityBtn)
    Button captureActivityBtn;
    @BindView(R.id.emptyVideoScreenGroup)
    Group emptyVideoScreenGroup;
    @BindView(R.id.myVideosListRecyclerView)
    RecyclerView myVideosListRecyclerView;
    @BindView(R.id.menuIcon)
    ImageView menuIcon;
    @BindView(R.id.menuDrawerLayout)
    MyDrawerLayout menuDrawerLayout;
    @BindView(R.id.navigationView)
    NavigationView navigationView;
    @BindView(R.id.termsOfUse)
    TextView termsOfUse;
    MyVideoAdapter myVideoAdapter;
    List<UserVideoList> userVideoLists = new ArrayList<>();
    ICollectionFragmentPresenter ICPresenter;
    @BindView(R.id.footer_item_1)
    TextView footer_item_10;
    @BindView(R.id.swiperefresh2)
    SwipeRefreshLayout swiperefresh2;
    private ActionBarDrawerToggle drawerToggle;
    private AppSharedPreference preference;

    public CollectionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_collections_video, container, false);
        ButterKnife.bind(this, v);
        bindEvents();
        String versionName = "";
        int versionCode = -1;
        try {
            PackageInfo packageInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            versionName = packageInfo.versionName;
            versionCode = packageInfo.versionCode;

            footer_item_10.setText("Version " + versionName);//+ " | ");// + "Terms of Use");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        handler = new Handler();
        preference = AppSharedPreference.getInstance();
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
        requireActivity().getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
        menuDrawerLayout.addDrawerListener(drawerToggle);
        attachPresenter();
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


        ICPresenter.getUserCollectionVideos(false);

        swiperefresh2.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                ICPresenter.getUserCollectionVideos(true);

            }
        });

        return v;
    }

    public void attachPresenter() {
        if (ICPresenter == null) {
            ICPresenter = new CollectionFragmentPresenter();
        }
        ICPresenter.onViewAttached(this, getActivity());
    }

    // TODO: Rename method, update argument and hook method into UI event
    @Override
    public void initializeVideoListAdapter(JSONArray videoObject) {
        userVideoLists.clear();
        UserVideoList userVideoList;
        if (videoObject.length() > 0) {
            for (int i = 0; i < videoObject.length(); i++) {
                try {
                    userVideoList = new UserVideoList(videoObject.getJSONObject(i));
                    userVideoLists.add(userVideoList);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            RecyclerView.LayoutManager manager = new LinearLayoutManager(getActivity());
            myVideosListRecyclerView.setLayoutManager(manager);
            myVideoAdapter = new MyVideoAdapter(userVideoLists, getActivity(), this, myVideosListRecyclerView, this);
            myVideoAdapter.setOnLoadMoreListener(new OnLoadMoreListener() {
                @Override
                public void onLoadMore() {


                    ICPresenter.getLoadMoreDatas(false);


                }
            });


            myVideosListRecyclerView.setAdapter(myVideoAdapter);
            if (Globals.isShowingLoader()) {
                Globals.dismissLoading();

            }
            if (Globals.isShowingLoader2()) {
                Globals.dismissLoading2();

            }
            if (swiperefresh2.isRefreshing()) {
                swiperefresh2.setRefreshing(false);
            }
            if (emptyVideoScreenGroup.getVisibility() == View.VISIBLE) {
                emptyVideoScreenGroup.setVisibility(View.GONE);
            }
        } else {
            emptyVideoScreenGroup.setVisibility(View.VISIBLE);
            if (Globals.isShowingLoader()) {
                Globals.dismissLoading();
            }
            if (Globals.isShowingLoader2()) {
                Globals.dismissLoading2();
            }
            if (swiperefresh2.isRefreshing()) {
                swiperefresh2.setRefreshing(false);
            }
        }
    }

    @Override
    public void initializeLoadMoreDataToAdapter(JSONArray videoObject) {

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                UserVideoList userVideoList;
                for (int i = 0; i < videoObject.length(); i++) {
                    try {
                        userVideoList = new UserVideoList(videoObject.getJSONObject(i));
                        userVideoLists.add(userVideoList);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                myVideoAdapter.setLoaded();
                myVideoAdapter.notifyDataSetChanged();
                if (Globals.isShowingLoader()) {
                    Globals.dismissLoading();
                }
           }
        }, 2000);
    }

    @Override
    public void bindEvents() {
        menuIcon.setOnClickListener(this);
        navigationView.setNavigationItemSelectedListener(this);
        captureActivityBtn.setOnClickListener(this);
        termsOfUse.setOnClickListener(this);
    }


    @Override
    public void onCollectionSelection(UserVideoList collectionsData, int position) {
        if (getActivity() == null) {
            return;
        }
        Intent intent = new Intent(getActivity(), VideoEditorActivity.class);
        intent.putExtra(Constant.VIDEO_CONTENT_KEY, true);
        intent.putExtra("jsonURL", collectionsData.getRaw_json_file_path());
        intent.putExtra("videoURL", collectionsData.getRaw_video_file_path());
        intent.putExtra("collectionId", collectionsData.getCollectionId());
        getActivity().startActivity(intent);
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

            case R.id.menuIcon:
                if (menuDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    menuDrawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    menuDrawerLayout.openDrawer(GravityCompat.START);
                }
                break;
            case R.id.captureActivityBtn:
                if (getActivity() != null) {
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
    public void onDestroy() {
        super.onDestroy();
        ICPresenter.onViewDetached();
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
                    getActivity().startActivityForResult(projectIntent, DashboardActivity.VIDEOS_REQUEST_CODE);
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
        emptyVideoScreenGroup.setVisibility(View.VISIBLE);
        if (Globals.isShowingLoader2()) {
            Globals.dismissLoading2();
        }
        if (Globals.isShowingLoader()) {
            Globals.dismissLoading();
        }
        if (swiperefresh2.isRefreshing()) {
            swiperefresh2.setRefreshing(false);
        }
        Globals.showSnackBar("No Data Available!", getActivity(), Snackbar.LENGTH_LONG);
    }

    @Override
    public void handleError(){
        if(Globals.isShowingLoader()){
            Globals.dismissLoading();
        }
        Toast.makeText(getActivity(), getResources().getString(R.string.unknown_error_message), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEditCollectionSelection(UserVideoList collectionsData, int position) {
        if (getActivity() == null) {
            return;
        }
        Intent intent = new Intent(getActivity(), VideoEditorActivity.class);
        intent.putExtra(Constant.VIDEO_CONTENT_KEY, true);
        intent.putExtra("jsonURL", collectionsData.getRaw_json_file_path());
        intent.putExtra("videoURL", collectionsData.getRaw_video_file_path());
        intent.putExtra("collectionId", collectionsData.getCollectionId());
        intent.putExtra(Constant.IS_VIDEO_EDITABLE, true);
        getActivity().startActivity(intent);
    }
}
