package com.visym.collector.dashboardmodule.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.visym.collector.R;
import com.visym.collector.capturemodule.interfaces.Listeners.OnSelectListner;
import com.visym.collector.dashboardmodule.IDashboardModule.ICaptureFragmentPresenter;
import com.visym.collector.dashboardmodule.IDashboardModule.ICaptureFragmentView;
import com.visym.collector.dashboardmodule.adapters.ProjectListAdapter;
import com.visym.collector.dashboardmodule.presenter.CaptureFragmentPresenter;
import com.visym.collector.model.ProjectDataList;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.Constant;
import com.visym.collector.utils.Globals;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class CaptureFragment extends Fragment implements OnSelectListner<ProjectDataList>,
        ICaptureFragmentView, SearchView.OnQueryTextListener {
    @BindView(R.id.searchImageBtn)
    SearchView searchView;
    @BindView(R.id.back)
    ImageView back;
    @BindView(R.id.projectListRecyclerView)
    RecyclerView projectListRecyclerView;
    ProjectListAdapter projectListAdapter;
    List<ProjectDataList> pDataList;
    ICaptureFragmentPresenter ICPresenter;

    public CaptureFragment() {
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
        View fragmentView = inflater.inflate(R.layout.fragment_capture, container, false);
        ButterKnife.bind(this, fragmentView);
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                searchView.setIconifiedByDefault(true);
                return false;
            }
        });
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });
        attachPresenter();
        Globals.showLoading(getActivity());
        ICPresenter.getProjectList();
        return fragmentView;
    }


    @Override
    public void onCollectionSelection(ProjectDataList collectionsData, int position) {
        if (getActivity() != null) {
            String project_id = collectionsData.getId() + "_" + collectionsData.getProjectName();
            AppSharedPreference.getInstance().storeValue(Constant.PROGRAM_ID_KEY, collectionsData.getId());
            AppSharedPreference.getInstance().storeValue(Constant.PROJECT_ID_KEY, collectionsData.getProjectId());
            AppSharedPreference.getInstance().storeValue("id", project_id);
            AppSharedPreference.getInstance().storeValue(Constant.PROJECT_NAME_KEY, collectionsData.getProjectName());
            Intent intent = new Intent();
            getActivity().setResult(Activity.RESULT_OK, intent);
            getActivity().finish();
        }
    }

    public void attachPresenter() {
        if (ICPresenter == null) {
            ICPresenter = new CaptureFragmentPresenter();
        }

        ICPresenter.onViewAttached(this, getActivity());
    }

    @Override
    public void initializeProjectListViews(JSONArray object) {
        pDataList = new ArrayList<>();
        ProjectDataList projectDataList;
        for (int i = 0; i < object.length(); i++) {
            try {
                Log.e(Globals.TAG, "initializeProjectListViews: " + object.toString());
                if (Integer.parseInt(object.getJSONObject(i).getString("collectionsCount")) > 0) {
                    projectDataList = new ProjectDataList(object.getJSONObject(i));
                    if (i == 0) {
                        if (AppSharedPreference.getInstance().readString(Constant.PROJECT_ID_KEY) == null) {
                            String project_id = projectDataList.getId() + "_" + projectDataList.getProjectName();
                            AppSharedPreference.getInstance().storeValue(Constant.PROGRAM_ID_KEY, projectDataList.getId());
                            AppSharedPreference.getInstance().storeValue("id", project_id);
                            AppSharedPreference.getInstance().storeValue(Constant.PROJECT_ID_KEY, projectDataList.getProjectId());
                            AppSharedPreference.getInstance().storeValue(Constant.PROJECT_NAME_KEY, projectDataList.getProjectName());
                        }
                    }
                    pDataList.add(projectDataList);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        projectListAdapter = new ProjectListAdapter(pDataList, getActivity(), this);
        RecyclerView.LayoutManager manager = new LinearLayoutManager(getActivity());
        projectListRecyclerView.setLayoutManager(manager);
        projectListRecyclerView.setAdapter(projectListAdapter);
        if (Globals.isShowingLoader()) {
            Globals.dismissLoading();
        }
    }


    @Override
    public void bindEvents() {
        // Binding events
    }

    @Override
    public void handleError() {
        if(Globals.isShowingLoader()){
            Globals.dismissLoading();
        }
        Toast.makeText(getActivity(), getResources().getString(R.string.unknown_error_message), Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (projectListAdapter != null) {
            projectListAdapter.getFilter().filter(query);
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (projectListAdapter != null) {
            projectListAdapter.getFilter().filter(newText);
        }
        return false;
    }
}
