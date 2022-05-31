package com.visym.collector.dashboardmodule.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amazonaws.amplify.generated.graphql.UpdateStrVideosMutation;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.visym.collector.R;
import com.visym.collector.capturemodule.interfaces.Listeners;
import com.visym.collector.dashboardmodule.OnLoadMoreListener;
import com.visym.collector.dashboardmodule.view.DashboardActivity;
import com.visym.collector.model.NetworkCallback;
import com.visym.collector.model.UserVideoList;
import com.visym.collector.network.ConnectivityReceiver;
import com.visym.collector.network.project.AwsS3Repository;
import com.visym.collector.utils.AwsResponse;
import com.visym.collector.utils.DateUtil;
import com.visym.collector.utils.Globals;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;

import butterknife.BindView;
import butterknife.ButterKnife;
import type.UpdateStrVideosInput;

public class MyVideoAdapter extends RecyclerView.Adapter{

  private Listeners.OnSelectListner selectListner;
  private Listeners.OnEditOptionSelectListner editSelectListner;
  private List<UserVideoList> userVideoLists;
  private Context mcontext;
  private RecyclerView videoRView;
  private final int VIEW_ITEM = 1;
  private final int VIEW_PROG = 0;
  private int visibleThreshold = 1;
  private int lastVisibleItem, totalItemCount,totalEmailFileSize = 0;
  private boolean loading;
  private OnLoadMoreListener onLoadMoreListener;
  private String convertedDate;


  public MyVideoAdapter(List<UserVideoList> dataset, Context context,
                        Listeners.OnSelectListner listner, RecyclerView videoRecyclerView, Listeners.OnEditOptionSelectListner editOptionListner) {
    this.userVideoLists = dataset;
    this.mcontext = context;
    this.selectListner = listner;
    this.videoRView = videoRecyclerView;
    this.editSelectListner = editOptionListner;


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
                  if (!loading && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                    if (onLoadMoreListener != null) {
                      if(dy!=0) {
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
              R.layout.my_videos_list_layout, parent, false);
      vh = new VideoViewHolder(v);
    } else {
      View v = LayoutInflater.from(parent.getContext()).inflate(
              R.layout.progress_bar, parent, false);
      vh = new ProgressViewHolder(v);
    }
    return vh;
  }

  @Override
  public int getItemViewType(int position) {
    return userVideoLists.get(position) != null ? VIEW_ITEM : VIEW_PROG;
  }


  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    if (holder instanceof VideoViewHolder) {
    UserVideoList list = userVideoLists.get(position);
    ((VideoViewHolder) holder).hashTag.setText(list.getCollection_name());
    ((VideoViewHolder) holder).videoCategoryDescription.setVisibility(View.GONE);
      String duration = list.getDuration();
      if(list.getVideo_state() != null){
        ((VideoViewHolder) holder).statusText.setText(list.getVideo_state());
      }else {
        ((VideoViewHolder) holder).statusText.setVisibility(View.GONE);
      }
      if (!TextUtils.isEmpty(duration)){
        ((VideoViewHolder) holder).durationText.setVisibility(View.VISIBLE);
        ((VideoViewHolder) holder).durationText
                .setText(DateUtil.convertToSecString(Double.valueOf(duration)));
      }else {
        ((VideoViewHolder) holder).durationText.setVisibility(View.GONE);
      }

      if(!list.getThumbnail_small().equals("empty")){
          Glide.with(mcontext).load(list.getThumbnail_small()).placeholder(R.drawable.vph1)
                  .fitCenter().into(((VideoViewHolder) holder).videoImagePreview);
      }else {
          ((VideoViewHolder) holder).processingImage.setVisibility(View.VISIBLE);
          ((VideoViewHolder) holder).processingLabelText.setText(mcontext.getString(R.string.processing_label));
          ((VideoViewHolder) holder).processingLabelText.setVisibility(View.VISIBLE);
      }

    if (!list.getRating().equals("0")) {
      String ratingPer = list.getRating() + "%";
      ((VideoViewHolder) holder).upVotePercentage.setText(ratingPer);
    }else {
        ((VideoViewHolder) holder).upVotePercentage.setVisibility(View.GONE);
        ((VideoViewHolder) holder).upVoteIcon.setVisibility(View.GONE);
    }


      SimpleDateFormat format1=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ");//"yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
      SimpleDateFormat format2=new SimpleDateFormat("MMM d, yyyy hh:mm a");

      if(list.getUploadedDate()!=null) {
        try {
          Date date = format1.parse(userVideoLists.get(position).getUploadedDate());
          convertedDate = format2.format(date);
        } catch (ParseException e) {
          e.printStackTrace();
        }
        ((VideoViewHolder) holder).uploadedDate.setText(convertedDate);
      }
  } else {
      ((ProgressViewHolder) holder).progressBar.setIndeterminate(true);
    }
}

  @Override
  public int getItemCount() {
    return userVideoLists.size();
  }

  public class VideoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    @BindView(R.id.videoImagePreview)
    ImageView videoImagePreview;
    @BindView(R.id.durationText)
    TextView durationText;
    @BindView(R.id.hashTag)
    TextView hashTag;
    @BindView(R.id.moreIcon)
    ImageView moreIcon;
    @BindView(R.id.statusText)
    TextView statusText;
    @BindView(R.id.videoCategoryDescription)
    TextView videoCategoryDescription;
    @BindView(R.id.upVoteIcon)
    ImageView upVoteIcon;
    @BindView(R.id.upVotePercentage)
    TextView upVotePercentage;
    @BindView(R.id.processingImage)
    ImageView processingImage;
    @BindView(R.id.processingLabelText)
    TextView processingLabelText;
    @BindView(R.id.uploadedDate)
    TextView uploadedDate;
    public VideoViewHolder(@NonNull View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
      moreIcon.setOnClickListener(this);
      videoImagePreview.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
      switch (v.getId()) {
        case R.id.moreIcon:
          final PopupMenu popup = new PopupMenu(mcontext, moreIcon);
          popup.getMenuInflater().inflate(R.menu.video_menu, popup.getMenu());
          popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
               if (item.getItemId() == R.id.share) {
                sharevideo(getAdapterPosition());
                return true;
             } else if (item.getItemId() == R.id.edit) {
                editSelectListner.onEditCollectionSelection(userVideoLists.get(getAdapterPosition()), getAdapterPosition());
                popup.dismiss();
                return false;
              } else
                if (item.getItemId() == R.id.delete) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mcontext);
                builder.setMessage("Are you sure you want to delete this video?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
                return false;
              } else {
                return onMenuItemClick(item);
              }
            }
          });
          popup.show();
          break;
        case R.id.videoImagePreview:
          selectListner.onCollectionSelection(userVideoLists.get(getAdapterPosition()), getAdapterPosition());
          break;
      }
    }

    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        switch (which){
          case DialogInterface.BUTTON_POSITIVE:
            if(ConnectivityReceiver.isConnected()) {

              Globals.showLoading(mcontext);
              UpdateStrVideosInput updateQueryAttribute =
                      UpdateStrVideosInput.builder()
                              .id(userVideoLists.get(getAdapterPosition()).getId())
                              .video_id(userVideoLists.get(getAdapterPosition()).getVideo_id())
                              .uploaded_date(userVideoLists.get(getAdapterPosition()).getUploadedDate())
                              .query_attribute("0").build();
              Globals.mAWSAppSyncClient.mutate(UpdateStrVideosMutation.builder()
                      .input(updateQueryAttribute).build())
                      .enqueue(successResponse);
            }else {
              Globals.showSnackBar(mcontext.getResources().getString(R.string.noInternet),mcontext,Snackbar.LENGTH_LONG);
            }
            dialog.dismiss();
            break;

          case DialogInterface.BUTTON_NEGATIVE:
            dialog.dismiss();
            break;
        }
      }
    };



    private GraphQLCall.Callback<UpdateStrVideosMutation.Data> successResponse = new GraphQLCall.Callback<UpdateStrVideosMutation.Data>() {
      @Override
      public void onResponse(@Nonnull Response<UpdateStrVideosMutation.Data> response) {
        ((DashboardActivity)mcontext).runOnUiThread(new Runnable() {
          @Override
          public void run() {
            userVideoLists.remove(getAdapterPosition());
            notifyItemRemoved(getAdapterPosition());
            if (Globals.isShowingLoader())
              Globals.dismissLoading();
            Globals.showSnackBar(mcontext.getResources().getString(R.string.delete_success),mcontext, Snackbar.LENGTH_LONG);
          }
        });
      }

      @Override
      public void onFailure(@Nonnull ApolloException e) {
        ((DashboardActivity)mcontext).runOnUiThread(new Runnable() {
          @Override
          public void run() {
            if (Globals.isShowingLoader())
              Globals.dismissLoading();
            Globals.showSnackBar(mcontext.getResources().getString(R.string.delete_failure),mcontext, Snackbar.LENGTH_LONG);
          }
        });
      }
    };
  }

  public static class ProgressViewHolder extends RecyclerView.ViewHolder {
    public ProgressBar progressBar;

    public ProgressViewHolder(View v) {
      super(v);
      progressBar = (ProgressBar) v.findViewById(R.id.progressBar1);
    }
  }


  public void setLoaded() {
    loading = false;
  }

  public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
    this.onLoadMoreListener = onLoadMoreListener;
  }

  public void sharevideo(int position){
    boolean isAppInstalled = appInstalledOrNot("com.google.android.gm");
    if(isAppInstalled) {
      if (userVideoLists.get(position).getVideo_sharing_link() != null && userVideoLists.get(position).getJson_sharing_link() != null) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        String[] strTo = {""};
        intent.putExtra(Intent.EXTRA_EMAIL, strTo);
        intent.putExtra(Intent.EXTRA_SUBJECT, mcontext.getString(R.string.share_email_hint_text));
        intent.putExtra(Intent.EXTRA_TEXT, "Video link: " + userVideoLists.get(position).getVideo_sharing_link()
                + "\n" + "Json Link: " + userVideoLists.get(position).getJson_sharing_link());

        intent.setType("message/rfc822");
        intent.setPackage("com.google.android.gm");
        mcontext.startActivity(intent);

      } else {
        String jsonFilePath = userVideoLists.get(position).getRaw_json_file_path();
        String videoFilePath = userVideoLists.get(position).getRaw_video_file_path();


        ((DashboardActivity) mcontext).showBaseActivityProgress();
        AwsS3Repository awsS3Repository = new AwsS3Repository();

        awsS3Repository.downloadFile(userVideoLists.get(position).getRaw_json_file_path(), false, new NetworkCallback<AwsResponse>() {
          @Override
          public void onSuccess(AwsResponse response) {
            TransferState state = response.getState();
            if (state == TransferState.COMPLETED) {
              downloadVideoFile(userVideoLists.get(position).getRaw_video_file_path(),response.getVideoUrl());
              ArrayList<Uri> uris = new ArrayList<Uri>();
              File fileIn = new File(response.getVideoUrl());
              Uri u = FileProvider.getUriForFile(mcontext, mcontext.getApplicationContext().getPackageName() + ".provider", fileIn);;
              uris.add(u);
            } else if (state == TransferState.FAILED) {
              ((DashboardActivity) mcontext).hideBaseActivityProgress();
              Toast.makeText(mcontext, response.getErrorMessage(), Toast.LENGTH_SHORT).show();
            } else if (state == TransferState.IN_PROGRESS) {
              int progress = response.getProgress();
              ((DashboardActivity) mcontext).updateBaseActivityProgress(progress);
            }
          }

          @Override
          public void onFailure(String error) {
            if (!TextUtils.isEmpty(error)) {
              Toast.makeText(mcontext, error, Toast.LENGTH_SHORT).show();
            }else {
              Toast.makeText(mcontext, "Something went wrong. Please try again later", Toast.LENGTH_SHORT).show();
            }
          }
        });

      }
    }
    else {
      Log.d("JEBYRNE", "Gmail app not installed");
    }
  }

  private void downloadVideoFile(String videoFilePath,String jsonFilePath){
    AwsS3Repository awsS3Repository = new AwsS3Repository();

    awsS3Repository.downloadFile(videoFilePath, false, new NetworkCallback<AwsResponse>() {
      @Override
      public void onSuccess(AwsResponse response) {
        TransferState state = response.getState();
        if (state == TransferState.COMPLETED) {
          String[] filePaths = new String[] {jsonFilePath,
                  response.getVideoUrl()};

          ArrayList<Uri> uris = new ArrayList<Uri>();
           for (String file : filePaths)
             {
          File fileIn = new File(file);
          totalEmailFileSize = totalEmailFileSize + Integer.parseInt(String.valueOf(fileIn.length()/1024));
          Uri u = FileProvider.getUriForFile(mcontext, mcontext.getApplicationContext().getPackageName() + ".provider", fileIn);;
          uris.add(u);
            }
           if((totalEmailFileSize/1000) < 25) {
             Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
             shareIntent.setType("text/plain");
             shareIntent.putExtra(Intent.EXTRA_SUBJECT, mcontext.getString(R.string.share_email_hint_text));
             shareIntent.putExtra(Intent.EXTRA_TEXT, "");
             shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
             mcontext.startActivity(Intent.createChooser(shareIntent, "choose one"));
           }else {
             Toast.makeText(mcontext, "file size is less than 25 mb", Toast.LENGTH_SHORT).show();
           }
          ((DashboardActivity) mcontext).hideBaseActivityProgress();
        } else if (state == TransferState.FAILED) {
          ((DashboardActivity) mcontext).hideBaseActivityProgress();
        } else if (state == TransferState.IN_PROGRESS) {
          int progress = response.getProgress();
          ((DashboardActivity) mcontext).updateBaseActivityProgress(progress);
        }
      }

      @Override
      public void onFailure(String error) {
        if (!TextUtils.isEmpty(error)) {
          Toast.makeText(mcontext, error, Toast.LENGTH_SHORT).show();
        }else {
          Toast.makeText(mcontext, "Something went wrong. Please try again later", Toast.LENGTH_SHORT).show();
        }
      }
    });
  }
  private boolean appInstalledOrNot(String uri) {
    PackageManager pm = mcontext.getPackageManager();
    try {
      // JEBYRNE: this no longer works in android 11
      pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
      return true;
    } catch (PackageManager.NameNotFoundException e) {
    }
    return false;
  }


}
