package com.visym.collector.dashboardmodule.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.visym.collector.R;
import com.visym.collector.capturemodule.interfaces.Listeners.OnSelectListner;
import com.visym.collector.model.ProjectDataList;
import com.visym.collector.utils.AppSharedPreference;
import com.visym.collector.utils.Constant;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ProjectListAdapter extends RecyclerView.Adapter<ProjectListAdapter.ListViewHolder> implements
        Filterable {

    private List<ProjectDataList> projectDataLists;
    private List<ProjectDataList> filteredProjectDataLists;
    private OnSelectListner projectSelectedListner;
    private Context mcontext;

    public ProjectListAdapter(List<ProjectDataList> projectDatas, Context context,
                              OnSelectListner<ProjectDataList> projectDataListOnSelectListner) {
        this.projectDataLists = projectDatas;
        this.filteredProjectDataLists = projectDatas;
        this.mcontext = context;
        this.projectSelectedListner = projectDataListOnSelectListner;
    }

    @NonNull
    @Override
    public ProjectListAdapter.ListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater
                .from(parent.getContext()).inflate(R.layout.project_list_layout, parent, false);
        return new ProjectListAdapter.ListViewHolder(v);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ProjectListAdapter.ListViewHolder holder, int position) {
        final ProjectDataList dataList = filteredProjectDataLists.get(position);
        if (dataList.getCollectionsCount() == null) {
            holder.activitiesCount.setText("0" + " Collections");
        } else {
            holder.activitiesCount.setText(dataList.getCollectionsCount() + " Collections");
        }
        holder.projectName.setText(dataList.getProjectName());
        if (dataList.getProjectName().equalsIgnoreCase(AppSharedPreference.getInstance().readString(Constant.PROJECT_NAME_KEY))) {
            holder.practiceProjectLayout.setBackgroundColor(mcontext.getResources().getColor(R.color.goldColor));
            holder.projectName.setTextColor(mcontext.getResources().getColor(R.color.textLightColor));
            holder.activitiesCount.setTextColor(mcontext.getResources().getColor(R.color.textLightColor));
            holder.pic.setBackgroundResource(R.drawable.circle_background_white);
            holder.circleCrop.setColorFilter(ContextCompat.getColor(mcontext, R.color.goldColor), android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            holder.practiceProjectLayout.setBackgroundColor(mcontext.getResources().getColor(R.color.textLightColor));
            holder.projectName.setTextColor(mcontext.getResources().getColor(R.color.darkBlack));
            holder.activitiesCount.setTextColor(mcontext.getResources().getColor(R.color.darkBlack));
            holder.pic.setBackgroundResource(R.drawable.circle_background);
            holder.circleCrop.setColorFilter(ContextCompat.getColor(mcontext, R.color.white), android.graphics.PorterDuff.Mode.SRC_IN);
        }

    }

    @Override
    public int getItemCount() {
        return filteredProjectDataLists.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String charString = constraint.toString();
                if (charString.isEmpty()) {
                    filteredProjectDataLists = projectDataLists;
                } else {
                    List<ProjectDataList> filteredList = new ArrayList<>();
                    for (ProjectDataList row : projectDataLists) {
                        if (row.getProjectName().toLowerCase().contains(charString.toLowerCase()))
                            filteredList.add(row);
                    }
                    filteredProjectDataLists = filteredList;
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredProjectDataLists;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredProjectDataLists = (ArrayList<ProjectDataList>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    public class ListViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.projectName)
        TextView projectName;
        @BindView(R.id.activitiesCount)
        TextView activitiesCount;
        @BindView(R.id.practiceProjectLayout)
        ConstraintLayout practiceProjectLayout;
        @BindView(R.id.pic)
        ImageView pic;
        @BindView(R.id.circle_crop)
        ImageView circleCrop;

        public ListViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    projectSelectedListner.onCollectionSelection(filteredProjectDataLists.get(getAdapterPosition()), getAdapterPosition());
                }
            });
        }
    }
}