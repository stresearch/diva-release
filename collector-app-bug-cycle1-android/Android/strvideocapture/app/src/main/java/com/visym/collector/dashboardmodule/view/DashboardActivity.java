package com.visym.collector.dashboardmodule.view;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.visym.collector.R;
import com.visym.collector.network.ConnectivityReceiver;
import com.visym.collector.network.ConnectivityReceiver.ConnectivityReceiverListener;
import com.visym.collector.utils.Globals;


import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DashboardActivity extends AppCompatActivity implements
        ConnectivityReceiverListener, View.OnClickListener {

    public static final int VIDEOS_REQUEST_CODE = 100;
    public static final int RATING_REQUEST_CODE = 100;
    @BindView(R.id.bottomNavigation)
    BottomNavigationView bottomNavigation;
    @BindView(R.id.frameLayout)
    FrameLayout frameLayout;
    MenuItem optionsMenu;
    Fragment redirectFragment = null;
    String[] array_nums = {"home", "capture", "gallery"};
    int[] idsinitial = {R.id.home, R.id.capture, R.id.gallery};


    ArrayList<String> integers = new ArrayList<>();
    ArrayList<Integer> ids = new ArrayList<>();
    String x = array_nums[0];
    private Context mcontext;
    private boolean isOnHomeScreen = false;
    private ProgressDialog progressDialog;

    public static int findIndex(String[] arr, String t) {
        if (arr == null) {
            return -1;
        }
        int len = arr.length;
        int i = 0;
        while (i < len) {
            if (arr[i] == t) {
                return i;
            } else {
                i = i + 1;
            }
        }
        return -1;
    }

    public static int findIndex2(Integer[] arr, int t) {
        if (arr == null) {
            return -1;
        }
        int len = arr.length;
        int i = 0;
        while (i < len) {
            if (arr[i] == t) {
                return i;
            } else {
                i = i + 1;
            }
        }
        return -1;
    }

    public static int findIndex3(Integer[] arr, int t) {
        if (arr == null) {
            return -1;
        }
        int len = arr.length;
        int i = 0;
        while (i < len) {
            if (arr[i] == t) {
                return i;
            } else {
                i = i + 1;
            }
        }
        return -1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        ButterKnife.bind(this);
        mcontext = this;
        integers.add("home");
        ids.add(idsinitial[0]);


        bottomNavigation.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                        if (ConnectivityReceiver.isConnected()) {
                            switch (menuItem.getItemId()) {
                                case R.id.home:

                                    if (integers.contains("home")) {
                                        int val = findIndex(array_nums, "home");
                                        int val2 = findIndex3(ids.toArray(new Integer[3]), R.id.home);

                                        if (!integers.get(0).equalsIgnoreCase("home")) {
                                            integers.add("home");
                                            ids.add(R.id.home);
                                        }

                                    }
                                    x = array_nums[0];
                                    menuItem.setIcon(getResources().getDrawable(R.drawable.home));
                                    bottomNavigation.getMenu().getItem(1)
                                            .setIcon(getResources().getDrawable(R.drawable.capture));
                                    bottomNavigation.getMenu().getItem(2)
                                            .setIcon(getResources().getDrawable(R.drawable.gallery));
                                    optionsMenu = menuItem;
                                    redirectToFragment("home");
                                    Log.e(Globals.TAG, "onNavigationItemSelected: home selected");
                                    break;
                                case R.id.capture:

                                    if (integers.contains("capture")) {
                                        int val = findIndex(integers.toArray(new String[3]), "capture");
                                        int val2 = findIndex3(ids.toArray(new Integer[3]), R.id.capture);

                                        if (integers.get(val).equalsIgnoreCase("capture")) {
                                            integers.remove(val);
                                            integers.add("capture");
                                            ids.remove(val2);
                                            ids.add(R.id.capture);
                                        }
                                    } else {
                                        integers.add("capture");
                                        ids.add(R.id.capture);
                                    }
                                    redirectToFragment("capture");
                                    bottomNavigation.getMenu().getItem(0)
                                            .setIcon(getResources().getDrawable(R.drawable.home_inactive));
                                    menuItem.setIcon(getResources().getDrawable(R.drawable.capture_active));
                                    bottomNavigation.getMenu().getItem(2)
                                            .setIcon(getResources().getDrawable(R.drawable.gallery));

                                    optionsMenu = menuItem;
                                    break;
                                case R.id.gallery:
                                    if (integers.contains("gallery")) {
                                        int val = findIndex(integers.toArray(new String[3]), "gallery");
                                        int val2 = findIndex3(ids.toArray(new Integer[3]), R.id.gallery);

                                        if (integers.get(val).equalsIgnoreCase("gallery")) {

                                            integers.remove(val);
                                            integers.add("gallery");
                                            ids.remove(val2);
                                            ids.add(R.id.gallery);
                                        }

                                    } else {
                                        integers.add("gallery");
                                        ids.add(R.id.gallery);


                                    }
                                    bottomNavigation.getMenu().getItem(0)
                                            .setIcon(getResources().getDrawable(R.drawable.home_inactive));
                                    bottomNavigation.getMenu().getItem(1)
                                            .setIcon(getResources().getDrawable(R.drawable.capture));
                                    menuItem.setIcon(getResources().getDrawable(R.drawable.gallery_active));
                                    redirectToFragment("gallery");
                                    optionsMenu = menuItem;
                                    break;
                            }
                        } else {
                            Globals.showSnackBar(getResources().getString(R.string.noInternet), mcontext,
                                    Snackbar.LENGTH_LONG);
                        }
                        return true;
                    }
                });


        if (ConnectivityReceiver.isConnected()) {
            if (getIntent().getExtras() != null) {
                Log.e(Globals.TAG, "onCreate: " + getIntent().getStringExtra("redirectTo"));
                if (getIntent().hasExtra("redirectTo") && !getIntent().getStringExtra("redirectTo").equalsIgnoreCase("gallery")) {
                    if (getIntent().getStringExtra("redirectTo").equalsIgnoreCase("capture")) {
                        redirectToFragment("capture");
                    } else {
                        redirectToFragment("home");
                    }
                } else {
                    redirectToFragment("gallery");
                }
            } else {
                redirectToFragment("home");
            }
            isOnHomeScreen = false;
        } else {
            isOnHomeScreen = false;
            Globals.showSnackBar(getResources().getString(R.string.noInternet), mcontext,
                    Snackbar.LENGTH_SHORT);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Globals.getInstance().setConnectivityListener(this);
    }

    public void redirectToFragment(String type) {
        if (type.equalsIgnoreCase("home")) {
            redirectFragment = new CollectionFragment();
            bottomNavigation.getMenu().findItem(R.id.home).setChecked(true);
            bottomNavigation.getMenu().getItem(1)
                    .setIcon(getResources().getDrawable(R.drawable.capture));
            bottomNavigation.getMenu().getItem(2)
                    .setIcon(getResources().getDrawable(R.drawable.gallery));
            isOnHomeScreen = true;
        } else if (type.equalsIgnoreCase("capture")) {
            redirectFragment = ProjectCollections.newInstance();
            bottomNavigation.getMenu().findItem(R.id.capture).setChecked(true);
            bottomNavigation.getMenu().getItem(1).setIcon(getResources().getDrawable(R.drawable.capture_active));
            bottomNavigation.getMenu().getItem(0).setIcon(getResources().getDrawable(R.drawable.home_inactive));
            bottomNavigation.getMenu().getItem(2).setIcon(getResources().getDrawable(R.drawable.gallery));
            isOnHomeScreen = false;
        } else if (type.equalsIgnoreCase("gallery")) {
            redirectFragment = new RatingFragment();
            bottomNavigation.getMenu().getItem(0)
                    .setIcon(getResources().getDrawable(R.drawable.home_inactive));
            bottomNavigation.getMenu().getItem(1)
                    .setIcon(getResources().getDrawable(R.drawable.capture));
            bottomNavigation.getMenu().findItem(R.id.gallery).setChecked(true);
            isOnHomeScreen = false;
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameLayout, redirectFragment)
                .commit();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String value = intent.getStringExtra("redirectTo");
        if (value != null){
            redirectToFragment(value);
        }
    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        if (!isConnected) {
            Globals.showSnackBar(getResources().getString(R.string.noInternet), mcontext,
                    Snackbar.LENGTH_INDEFINITE);
        } else {
            if (isOnHomeScreen) {
                isOnHomeScreen = true;
                redirectToFragment("home");
            }
            Globals.showSnackBar(getResources().getString(R.string.internet), mcontext,
                    Snackbar.LENGTH_LONG);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Globals.getInstance().unregisterConnectivityListener();
    }

    @Override
    public void onBackPressed() {
        Log.d("JEBYRNE", "DashboardActivity.onBackPressed");
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        int seletedItemId = bottomNavigationView.getSelectedItemId();
        Integer[] item = ids.toArray(new Integer[ids.size()]);
        int val = findIndex2(item, seletedItemId);
        if (ids.contains(seletedItemId)) {
            if (R.id.home != seletedItemId) {
                if (val != 0) {
                    setHomeItem(DashboardActivity.this, ids.get(val - 1));
                } else {
                    setHomeItem(DashboardActivity.this, ids.get(val));
                }
            } else {
                integers.clear();
                ids.clear();
                super.onBackPressed();
            }
        } else {
            integers.clear();
            ids.clear();
            super.onBackPressed();
        }
    }

    public void setHomeItem(Activity activity, int id) {
        if (id == R.id.home) {
            bottomNavigation.getMenu().getItem(0).setIcon(getResources().getDrawable(R.drawable.home));
            bottomNavigation.getMenu().getItem(1)
                    .setIcon(getResources().getDrawable(R.drawable.capture));
            bottomNavigation.getMenu().getItem(2)
                    .setIcon(getResources().getDrawable(R.drawable.gallery));
            redirectToFragment("home");

        } else if (id == R.id.capture) {
            bottomNavigation.getMenu().getItem(0)
                    .setIcon(getResources().getDrawable(R.drawable.home_inactive));
            bottomNavigation.getMenu().getItem(1).setIcon(getResources().getDrawable(R.drawable.capture_active));
            bottomNavigation.getMenu().getItem(2)
                    .setIcon(getResources().getDrawable(R.drawable.gallery));
            redirectToFragment("capture");
        } else if (id == R.id.gallery) {
            bottomNavigation.getMenu().getItem(0)
                    .setIcon(getResources().getDrawable(R.drawable.home_inactive));
            bottomNavigation.getMenu().getItem(1)
                    .setIcon(getResources().getDrawable(R.drawable.capture));
            bottomNavigation.getMenu().getItem(2).setIcon(getResources().getDrawable(R.drawable.gallery_active));
            redirectToFragment("gallery");
        }
        if (id == R.id.home) {
            integers.clear();
            ids.clear();
            integers.add("home");
            ids.add(R.id.home);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.menuIcon:
                Toast.makeText(mcontext, "MenuIcon ", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            integers.clear();
            ids.clear();
            integers.add("home");
            integers.add("capture");
            ids.add(R.id.home);
            ids.add(R.id.capture);
            integers.add("gallery");
            ids.add(R.id.gallery);
            redirectToFragment("capture");
        }
    }

    public void showBaseActivityProgress() {
        showProgress("Downloading video please wait...", true);
    }

    public void hideBaseActivityProgress() {
        hideProgress();
    }

    public void updateBaseActivityProgress(int progress) {
        updateProgress(progress);
    }

    public void showProgress(String message, boolean progress) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
            progressDialog.setMessage(message);
            if (progress) {
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setMax(100);
            }
        }
        progressDialog.show();
    }

    public void updateProgress(int progress) {
        if (progressDialog != null) {
            progressDialog.setProgress(progress);
        }
    }

    public void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        Globals.dismissLoading();
        super.onDestroy();
    }
}
