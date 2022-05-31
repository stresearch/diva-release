package com.visym.collector.capturemodule.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.visym.collector.R;
import com.visym.collector.capturemodule.interfaces.Listeners;
import com.visym.collector.model.CollectionModel;
import com.visym.collector.utils.Globals;
import java.util.ArrayList;
import java.util.List;

public class CollectionsListAdapter extends RecyclerView.Adapter<CollectionsListAdapter.CollectionsViewHolder> implements
    Filterable {
  private List<CollectionModel> collectionsData ;
  private List<CollectionModel> filteredCollectionsData ;
  private Listeners.OnSelectListner onSelectListner;
  private Context mcontext;
  private int rowIndex;
  public CollectionsListAdapter(List<CollectionModel> collectionList, Listeners.OnSelectListner listener,Context context) {

    this.onSelectListner = listener;
    this.collectionsData = collectionList;
    this.filteredCollectionsData = collectionList;
    this.mcontext  = context;
  }
  @NonNull
  @Override
  public CollectionsListAdapter.CollectionsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View itemView = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.collections_list_adapter_layout, parent, false);

    return new CollectionsViewHolder(itemView);
  }

  @Override
  public void onBindViewHolder(@NonNull CollectionsListAdapter.CollectionsViewHolder holder, int position) {
    final CollectionModel collectionData = filteredCollectionsData.get(position);
    holder.collectionName.setText(capitalizeEachWord(collectionData.getCollectionName()));
    holder.collectionName.setId(position);
    holder.collectionName.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        rowIndex = position;
        onSelectListner.onCollectionSelection(filteredCollectionsData.get(position), position);
        notifyDataSetChanged();
      }

    });
    if(rowIndex == position){
      holder.collectionName.setBackgroundColor(mcontext.getResources().getColor(R.color.goldColor));
      holder.collectionName.setTextColor(mcontext.getResources().getColor(R.color.textLightColor));
    }else {
      holder.collectionName.setBackgroundColor(mcontext.getResources().getColor(R.color.textLightColor));
      holder.collectionName.setTextColor(mcontext.getResources().getColor(R.color.darkBlack));
    }
  }

  @Override
  public int getItemCount() {

    Log.e(Globals.TAG, "getItemCount: "+filteredCollectionsData.size());
    return filteredCollectionsData.size();
  }
  public String capitalizeEachWord(String words){
    String str = words;
    String[] strArray = str.split(" ");
    StringBuilder builder = new StringBuilder();
    for (String s : strArray) {
      String cap = s.substring(0, 1).toUpperCase() + s.substring(1);
      builder.append(cap + " ");
    }
    return builder.toString();
  }
  @Override
  public Filter getFilter() {

    return new Filter() {
      @Override
      protected FilterResults performFiltering(CharSequence constraint) {
        String charString = constraint.toString();

        if(charString.isEmpty()){
          filteredCollectionsData = collectionsData;
        }else {
          List<CollectionModel> filteredList = new ArrayList<>();

          for(CollectionModel row : collectionsData){
            if(row.getCollectionName().toLowerCase().contains(charString.toLowerCase()))
              filteredList.add(row);
          }
          filteredCollectionsData = filteredList;
        }
        FilterResults filterResults = new FilterResults();
        filterResults.values = filteredCollectionsData;
        return filterResults;
      }

      @Override
      protected void publishResults(CharSequence constraint, FilterResults results) {
        filteredCollectionsData = (ArrayList<CollectionModel>)results.values;
        notifyDataSetChanged();
      }
    };
  }

  public class CollectionsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    @BindView(R.id.collectionName)
    TextView collectionName;

    public CollectionsViewHolder(@NonNull View itemView) {
      super(itemView);
      ButterKnife.bind(this,itemView);
      collectionName.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

      onSelectListner.onCollectionSelection(filteredCollectionsData.get(getAdapterPosition()), getAdapterPosition());
    }
  }
}