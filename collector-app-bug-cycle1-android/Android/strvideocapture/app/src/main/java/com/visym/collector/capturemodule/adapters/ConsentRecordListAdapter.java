package com.visym.collector.capturemodule.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.visym.collector.R;
import com.visym.collector.capturemodule.interfaces.Listeners;

import com.visym.collector.model.ConsentDataQuestionnaire;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ConsentRecordListAdapter extends RecyclerView.Adapter<ConsentRecordListAdapter.ConsentDataListViewHolder>{
    List<ConsentDataQuestionnaire> consentRecords;
    Context adapterContext;
    Listeners.PositionListener positionListener;

    public ConsentRecordListAdapter(List<ConsentDataQuestionnaire> consentDatas, Listeners.PositionListener listener, Context context){
        this.consentRecords = consentDatas;
        this.positionListener = listener;
        this.adapterContext = context;
    }

    @NonNull
    @Override
    public ConsentRecordListAdapter.ConsentDataListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View consentDataListViewHolder  = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.consent_data_list,parent,false);
        return new ConsentRecordListAdapter.ConsentDataListViewHolder(consentDataListViewHolder);
    }

    @Override
    public void onBindViewHolder(@NonNull ConsentRecordListAdapter.ConsentDataListViewHolder holder, int position) {
        ConsentDataQuestionnaire consentQuestions = consentRecords.get(position);
        holder.consentText.setText(consentQuestions.getShortDescription());
        setAnimation(holder.itemView,position);
        positionListener.getAdapterPositionFromConsentList(position);
    }

    @Override
    public int getItemCount() {
        return consentRecords.size();
    }

    private void setAnimation(View viewToAnimate, int position) {
        Animation animation = AnimationUtils.loadAnimation(adapterContext, android.R.anim.slide_in_left);
        viewToAnimate.startAnimation(animation);
    }

    public class ConsentDataListViewHolder extends RecyclerView.ViewHolder{
        @BindView(R.id.consentText)
        TextView consentText;
        public ConsentDataListViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this,itemView);
        }
    }
}