package com.visym.collector.capturemodule.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.visym.collector.R;
import com.visym.collector.capturemodule.interfaces.Listeners;
import com.visym.collector.capturemodule.model.DataModel;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ConsentSubjectListAdapter extends RecyclerView.Adapter<ConsentSubjectListAdapter.SubjectViewHolder>{
    List<DataModel.ConsentSubjectList> consentSubjectLists;
    Listeners.OnSelectListner selectListner;
    Context mcontext;

    public ConsentSubjectListAdapter(List<DataModel.ConsentSubjectList> dataSet, Context context, Listeners.OnSelectListner listner){
        this.consentSubjectLists = dataSet;
        this.mcontext = context;
        this.selectListner = listner;
    }

    @NonNull
    @Override
    public ConsentSubjectListAdapter.SubjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View subjectListView = LayoutInflater.from(parent.getContext()).inflate(R.layout.consent_subject_email_list,parent,false);
        return new SubjectViewHolder(subjectListView);
    }

    @Override
    public void onBindViewHolder(@NonNull ConsentSubjectListAdapter.SubjectViewHolder holder, int position) {
        DataModel.ConsentSubjectList list = consentSubjectLists.get(position);
        holder.subjectemail.setText(list.getSubjectEmail());
    }

    @Override
    public int getItemCount() {
        return consentSubjectLists.size();
    }

    public class SubjectViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.subjectEmailText)
        TextView subjectemail;
        public SubjectViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this,itemView);
            subjectemail.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            selectListner.onCollectionSelection(consentSubjectLists.get(getAdapterPosition()), getAdapterPosition());
        }
    }
}
