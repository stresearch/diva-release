package com.visym.collector.capturemodule.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.visym.collector.R;
import com.visym.collector.model.CollectionActivitiesModel;
import java.util.List;

public class CollectionActivitiesAdapter extends RecyclerView.Adapter<CollectionActivitiesAdapter.ActivitiesViewHolder>{

  private List<CollectionActivitiesModel> activitiesData;
  private Context adapterContext;
  public CollectionActivitiesAdapter(List<CollectionActivitiesModel> activities ,Context context){
    this.activitiesData = activities;
    this.adapterContext = context;
  }
  @NonNull
  @Override
  public CollectionActivitiesAdapter.ActivitiesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View activitiesView = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.collection_activities_list_adapter,parent,false);
    return new ActivitiesViewHolder(activitiesView);
  }

  @Override
  public void onBindViewHolder(@NonNull CollectionActivitiesAdapter.ActivitiesViewHolder holder, int position) {
    final CollectionActivitiesModel activitiesModel = activitiesData.get(position);
    holder.activityName.setText(activitiesModel.getActivitiesName());
  }

  @Override
  public int getItemCount() {
    return activitiesData.size();
  }

  public class ActivitiesViewHolder extends RecyclerView.ViewHolder{
    @BindView(R.id.collectionActivitiesName)
    TextView activityName;

    public ActivitiesViewHolder(@NonNull View itemView) {
      super(itemView);
      ButterKnife.bind(this,itemView);
    }
  }
}
