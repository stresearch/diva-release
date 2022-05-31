package com.visym.collector.capturemodule.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.visym.collector.R;
import com.visym.collector.capturemodule.interfaces.Listeners;
import com.visym.collector.capturemodule.model.FrameActivity;
import com.visym.collector.utils.Globals;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ActivitiesDisplayAdapter extends RecyclerView.Adapter<ActivitiesDisplayAdapter.ViewHolder> {

    private static final String TAG = "ActivitiesDisplay";
    private final List<FrameActivity> activities;
    private int currentSelection = -1;
    private int previousSelection = -1;
    private Listeners.VideoCaptureActivityListener activityListener;
    private String gestureType;

    public ActivitiesDisplayAdapter(List<FrameActivity> activities, Listeners.VideoCaptureActivityListener activityListener) {
        this.activities = activities;
        this.activityListener = activityListener;
    }

    @NonNull
    @Override
    public ActivitiesDisplayAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activities_textview_layout,
                parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivitiesDisplayAdapter.ViewHolder holder, int position) {
        FrameActivity frameActivity = activities.get(position);
        holder.activityTextView.setText(frameActivity.getName());
        if (currentSelection != -1){
            if (position == currentSelection) {
                holder.activityTextView.setBackgroundResource(R.drawable.view_gold_baground);
            } else {
                holder.activityTextView.setBackgroundResource(R.drawable.view_white_border);
            }
        }else {
            holder.activityTextView.setBackgroundResource(R.drawable.view_white_border);
        }
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    public String getSelectedActivityLabel(int position) {
        if (position < activities.size()) {
            return activities.get(position).getName();
        }
        return null;
    }

    public void getUpdatedGesturePosition(String gesture) {
        this.gestureType = gesture;
        if (gestureType.equals("single")){
            if (currentSelection == activities.size() - 1) {
                previousSelection = -1;
                currentSelection = -1;
            } else if (currentSelection == -1){
                if (previousSelection == -1){
                    currentSelection++;
                }else if (previousSelection == 2){
                    previousSelection = -1;
                    currentSelection++;
                }else if (previousSelection == activities.size() - 1){
                    previousSelection = -1;
                    currentSelection = 0;
                } else  {
                    currentSelection = previousSelection;
                    currentSelection++;
                }
            }else {
                previousSelection = currentSelection;
                currentSelection++;
            }
        }else {
            if (currentSelection == -1 && previousSelection == -1){

            }else {
                if (currentSelection != -1){
                    previousSelection = currentSelection;
                    currentSelection = -1;
                }else {
                    currentSelection = previousSelection;
                    previousSelection = -1;
                }
            }
        }
        notifyDataSetChanged();
    }

    public int getSelectedPosition() {
        return currentSelection;
    }

    public int getPreviousPosition() {
        return previousSelection;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.activity_textview)
        TextView activityTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            activityTextView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            if (currentSelection == position){
                previousSelection = currentSelection;
                currentSelection = -1;
            }else {
                currentSelection = position;
                previousSelection = position - 1;
            }
            activityListener.onActivityClick(currentSelection);
            activityListener.updateLabels(currentSelection);
            notifyDataSetChanged();
        }
    }
}
