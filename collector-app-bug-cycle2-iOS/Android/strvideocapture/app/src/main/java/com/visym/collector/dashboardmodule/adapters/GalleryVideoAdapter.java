package com.visym.collector.dashboardmodule.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.visym.collector.R;
import com.visym.collector.capturemodule.interfaces.Listeners;
import com.visym.collector.capturemodule.interfaces.Listeners.OnSelectListner;
import com.visym.collector.dashboardmodule.OnLoadMoreListener;
import com.visym.collector.model.GalleryVideoList;
import com.visym.collector.utils.DateUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GalleryVideoAdapter extends RecyclerView.Adapter {

    private final int VIEW_ITEM = 1;
    private final int VIEW_PROG = 0;
    private Listeners.OnSelectListner selectListner;
    private List<GalleryVideoList> videoDataLists;
    private Context context;
    private RecyclerView videoRView;
    private int visibleThreshold = 1;
    private int lastVisibleItem, totalItemCount;
    private boolean loading;
    private OnLoadMoreListener onLoadMoreListener;
    private String convertedDate;

    public GalleryVideoAdapter(List<GalleryVideoList> videoLists, OnSelectListner<GalleryVideoList> listner
            , Context context, RecyclerView videoRecyclerView) {
        this.videoDataLists = videoLists;
        this.selectListner = listner;
        this.context = context;
        this.videoRView = videoRecyclerView;


        if (videoRView.getLayoutManager() instanceof LinearLayoutManager) {
            final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) videoRView
                    .getLayoutManager();
            videoRView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView,
                                       int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    totalItemCount = linearLayoutManager.getItemCount();
                    lastVisibleItem = linearLayoutManager
                            .findLastVisibleItemPosition();
                    if (!loading
                            && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                        if (onLoadMoreListener != null) {
                            if (dy != 0) {
                                onLoadMoreListener.onLoadMore();
                            }
                        }
                        loading = true;
                    }
                }
            });
        }
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                      int viewType) {
        RecyclerView.ViewHolder vh;
        if (viewType == VIEW_ITEM) {
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.gallery_video_list, parent, false);
            vh = new GalleryViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.progress_bar, parent, false);
            vh = new ProgressViewHolder(v);
        }
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder,
                                 int position) {
        if (holder instanceof GalleryViewHolder) {
            GalleryVideoList videoList = videoDataLists.get(position);
            ((GalleryViewHolder) holder).hashTag.setText(videoList.getCollection_name());
            String duration = videoList.getDuration();
            if (!TextUtils.isEmpty(duration)) {
                ((GalleryViewHolder) holder).durationText.setVisibility(View.VISIBLE);
                ((GalleryViewHolder) holder).durationText
                        .setText(DateUtil.convertToSecString(Double.valueOf(duration)));
            } else {
                ((GalleryViewHolder) holder).durationText.setVisibility(View.GONE);
            }
            Glide.with(context).load(videoList.getThumbnail_small()).fitCenter()
                    .placeholder(R.drawable.vph1).into(((GalleryViewHolder) holder).videoImagePreview);
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ");
            SimpleDateFormat format2 = new SimpleDateFormat("MMM d, yyyy hh:mm a");

            if (videoList.getUploaded_date() != null) {
                try {
                    Date date = format1.parse(videoList.getUploaded_date());
                    convertedDate = format2.format(date);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                ((GalleryViewHolder) holder).uploadedDate.setText(convertedDate);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return videoDataLists.get(position) != null ? VIEW_ITEM : VIEW_PROG;
    }

    @Override
    public int getItemCount() {
        return videoDataLists.size();
    }

    public void setLoaded() {
        loading = false;
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    public static class ProgressViewHolder extends RecyclerView.ViewHolder {
        public ProgressBar progressBar;

        public ProgressViewHolder(View v) {
            super(v);
            progressBar = v.findViewById(R.id.progressBar1);
        }
    }

    public class GalleryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.videoImagePreview)
        ImageView videoImagePreview;
        @BindView(R.id.durationText)
        TextView durationText;
        @BindView(R.id.hashTag)
        TextView hashTag;
        @BindView(R.id.videoCategoryDescription)
        TextView videoCategoryDescription;
        @BindView(R.id.upVoteIcon)
        ImageView upVoteIcon;
        @BindView(R.id.upVotePercentage)
        TextView upVotePercentage;
        @BindView(R.id.uploadedDate)
        TextView uploadedDate;

        public GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            videoImagePreview.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            selectListner.onCollectionSelection(videoDataLists.get(getAdapterPosition()),
                    getAdapterPosition());
        }
    }
}
