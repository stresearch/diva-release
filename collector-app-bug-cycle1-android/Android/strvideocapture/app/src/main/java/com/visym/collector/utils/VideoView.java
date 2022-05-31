package com.visym.collector.utils;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;

import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;


import androidx.annotation.RequiresApi;
import com.visym.collector.R;
import java.io.IOException;
import java.util.Map;
public class VideoView extends SurfaceView implements MediaController.MediaPlayerControl {
  private static final String TAG = "VideoView";

  // all possible internal states
  private static final int STATE_ERROR = -1;
  private static final int STATE_IDLE = 0;
  private static final int STATE_PREPARING = 1;
  private static final int STATE_PREPARED = 2;
  private static final int STATE_PLAYING = 3;
  private static final int STATE_PAUSED = 4;
  private static final int STATE_PLAYBACK_COMPLETED = 5;

  // settable by the client
  private Uri mUri;
  private Map<String, String> mHeaders;

  // mCurrentState is a VideoView object's current state.
  // mTargetState is the state that a method caller intends to reach.
  // For instance, regardless the VideoView object's current state,
  // calling pause() intends to bring the object to a target state
  // of STATE_PAUSED.
  private int mCurrentState = STATE_IDLE;
  private int mTargetState = STATE_IDLE;

  // All the stuff we need for playing and showing a video
  private SurfaceHolder mSurfaceHolder = null;
  private MediaPlayer mMediaPlayer = null;
  private int mAudioSession;
  private int mVideoWidth;
  private int mVideoHeight;
  private int mSurfaceWidth;
  private int mSurfaceHeight;
  private MediaController mMediaController;
  private int mCurrentBufferPercentage;
  private MediaPlayListener mMediaPlayListener;
  private int mSeekWhenPrepared;  // recording the seek position while preparing
  private boolean mCanPause;
  private boolean mCanSeekBack;
  private boolean mCanSeekForward;

  public VideoView(Context context) {
    super(context);
    initVideoView();
  }

  public VideoView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initVideoView();
  }

  public VideoView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initVideoView();
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public VideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    initVideoView();
  }

  private void initVideoView() {
    mVideoWidth = 0;
    mVideoHeight = 0;

    getHolder().addCallback(mSHCallback);
    getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    setFocusable(true);
    setFocusableInTouchMode(true);
    requestFocus();

    mCurrentState = STATE_IDLE;
    mTargetState = STATE_IDLE;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    Log.e(TAG, "onMeasure: widthMeasureSpec : "+widthMeasureSpec + " heightMeasureSpec : " +heightMeasureSpec);
    Log.e(TAG, "onMeasure: mVideoWidth : "+mVideoWidth + " mVideoHeight : " +mVideoHeight);
    int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
    int height = getDefaultSize(mVideoHeight, heightMeasureSpec);

    if (mVideoWidth > 0 && mVideoHeight > 0) {
      if (mVideoWidth * height > width * mVideoHeight) {

        height = width * mVideoHeight / mVideoWidth;
      } else if (mVideoWidth * height < width * mVideoHeight) {

        width = height * mVideoWidth / mVideoHeight;
      }
    }

    setMeasuredDimension(width, height);
  }

  @Override
  public CharSequence getAccessibilityClassName() {
    return VideoView.class.getName();
  }

  public int resolveAdjustedSize(int desiredSize, int measureSpec) {
    return getDefaultSize(desiredSize, measureSpec);
  }

  /**
   * Sets video path.
   *
   * @param path the path of the video.
   */
  public void setVideoPath(String path) {
    setVideoURI(Uri.parse(path));
  }

  /**
   * Sets video URI.
   *
   * @param uri the URI of the video.
   */
  public void setVideoURI(Uri uri) {
    setVideoURI(uri, null);
  }

  /**
   * Sets video URI using specific headers.
   *
   * @param uri     the URI of the video.
   * @param headers the headers for the URI request.
   *                Note that the cross domain redirection is allowed by default, but that can be
   *                changed with key/value pairs through the headers parameter with
   *                "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value
   *                to disallow or allow cross domain redirection.
   */
  public void setVideoURI(Uri uri, Map<String, String> headers) {
    mUri = uri;
    mHeaders = headers;
    mSeekWhenPrepared = 0;
    openVideo();
    requestLayout();
    invalidate();
  }

  public void stopPlayback() {
    if (mMediaPlayer != null) {
      mMediaPlayer.stop();
      mMediaPlayer.release();
      mMediaPlayer = null;
      mCurrentState = STATE_IDLE;
      mTargetState = STATE_IDLE;
      AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
      am.abandonAudioFocus(null);
    }
  }

  private void openVideo() {
    if (mUri == null || mSurfaceHolder == null) {
      // not ready for playback just yet, will try again later
      return;
    }
    // we shouldn't clear the target state, because somebody might have
    // called start() previously
    release(false);

    AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
    am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

    try {
      mMediaPlayer = PolicyCompat.getMediaPlayer(getContext());

      if (mAudioSession != 0) {
        mMediaPlayer.setAudioSessionId(mAudioSession);
      } else {
        mAudioSession = mMediaPlayer.getAudioSessionId();
      }
      mMediaPlayer.setOnPreparedListener(mPreparedListener);
      mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
      mMediaPlayer.setOnCompletionListener(mCompletionListener);
      mMediaPlayer.setOnErrorListener(mErrorListener);
      mMediaPlayer.setOnInfoListener(mInfoListener);
      mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
      mCurrentBufferPercentage = 0;
      mMediaPlayer.setDataSource(getContext(), mUri, mHeaders);
      mMediaPlayer.setDisplay(mSurfaceHolder);
      mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
      mMediaPlayer.setScreenOnWhilePlaying(true);
      mMediaPlayer.prepareAsync();

      // we don't set the target state here either, but preserve the
      // target state that was there before.
      mCurrentState = STATE_PREPARING;
      attachMediaController();
    } catch (IOException | IllegalArgumentException ex) {
      Log.w(TAG, "Unable to open content: " + mUri, ex);
      mCurrentState = STATE_ERROR;
      mTargetState = STATE_ERROR;
      mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
    }
  }

  public void setMediaController(MediaController controller) {
    if (mMediaController != null) {
      mMediaController.hide();
    }
    mMediaController = controller;
    attachMediaController();
  }

  private void attachMediaController() {
    if (mMediaPlayer != null && mMediaController != null) {
      mMediaController.setMediaPlayer(this);
      View anchorView = this.getParent() instanceof View ?
          (View) this.getParent() : this;
      mMediaController.setAnchorView(anchorView);
      mMediaController.setEnabled(isInPlaybackState());

    }
  }

  MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
      new MediaPlayer.OnVideoSizeChangedListener() {
        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
          mVideoWidth = mp.getVideoWidth();
          mVideoHeight = mp.getVideoHeight();
          if (mVideoWidth != 0 && mVideoHeight != 0) {
            getHolder().setFixedSize(mVideoWidth, mVideoHeight);
            requestLayout();
          }
        }
      };

  MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
    public void onPrepared(MediaPlayer mp) {
      mCurrentState = STATE_PREPARED;

      mCanPause = mCanSeekBack = mCanSeekForward = true;

      if (mMediaPlayListener != null) {
        mMediaPlayListener.onPrepared(mMediaPlayer);
      }
      if (mMediaController != null) {
        mMediaController.setEnabled(true);
      }
      mVideoWidth = mp.getVideoWidth();
      mVideoHeight = mp.getVideoHeight();

      int seekToPosition = mSeekWhenPrepared;  // mSeekWhenPrepared may be changed after seekTo() call
      if (seekToPosition != 0) {
        seekTo(seekToPosition);
      }
      if (mVideoWidth != 0 && mVideoHeight != 0) {
        getHolder().setFixedSize(mVideoWidth, mVideoHeight);
        if (mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight) {
          // We didn't actually change the size (it was already at the size
          // we need), so we won't get a "surface changed" callback, so
          // start the video here instead of in the callback.
          if (mTargetState == STATE_PLAYING) {
            start();
            if (mMediaController != null) {
              mMediaController.show(0);
            }
          } else if (!isPlaying() &&
              (seekToPosition != 0 || getCurrentPosition() > 0)) {
            if (mMediaController != null) {
              // Show the media controls when we're paused into a video and make 'em stick.
              mMediaController.show(0);
            }
          }
        }
      } else {
        // We don't know the video size yet, but should start anyway.
        // The video size might be reported to us later.
        if (mTargetState == STATE_PLAYING) {
          start();
        }
      }
    }
  };

  private MediaPlayer.OnCompletionListener mCompletionListener =
      new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
          mCurrentState = STATE_PLAYBACK_COMPLETED;
          mTargetState = STATE_PLAYBACK_COMPLETED;
          if (mMediaController != null) {
            mMediaController.show(0);
          }
          if (mMediaPlayListener != null) {
            mMediaPlayListener.onCompletion(mMediaPlayer);
          }
        }
      };

  private MediaPlayer.OnInfoListener mInfoListener =
      new MediaPlayer.OnInfoListener() {
        public boolean onInfo(MediaPlayer mp, int arg1, int arg2) {
          if (mMediaPlayListener != null) {
            mMediaPlayListener.onInfo(mp, arg1, arg2);
          }
          return true;
        }
      };

  private MediaPlayer.OnErrorListener mErrorListener =
      new MediaPlayer.OnErrorListener() {
        public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
          Log.d(TAG, "Error: " + framework_err + "," + impl_err);
          mCurrentState = STATE_ERROR;
          mTargetState = STATE_ERROR;
          if (mMediaController != null) {
            mMediaController.hide();
          }

          /* If an error handler has been supplied, use it and finish. */
          if (mMediaPlayListener != null) {
            if (mMediaPlayListener.onError(mMediaPlayer, framework_err, impl_err)) {
              return true;
            }
          }

          /* Otherwise, pop up an error dialog so the user knows that
           * something bad has happened. Only try and pop up the dialog
           * if we're attached to a window. When we're going away and no
           * longer have a window, don't bother showing the user an error.
           */
          if (getWindowToken() != null) {
            int messageId;

            if (framework_err == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
              messageId = R.string.VideoView_error_text_invalid_progressive_playback;
            } else {
              messageId = R.string.VideoView_error_text_unknown;
            }

            new AlertDialog.Builder(getContext())
                .setMessage(messageId)
                .setPositiveButton(R.string.VideoView_error_button,
                    new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog, int whichButton) {
                        /* If we get here, there is no onError listener, so
                         * at least inform them that the video is over.
                         */
                        if (mMediaPlayListener != null) {
                          mMediaPlayListener.onCompletion(mMediaPlayer);
                        }
                      }
                    })
                .setCancelable(false)
                .show();
          }
          return true;
        }
      };

  private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
      new MediaPlayer.OnBufferingUpdateListener() {
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
          mCurrentBufferPercentage = percent;
        }
      };

  /**
   * Register a callback to be invoked when player status change
   * @param l
   */
  public void setMediaPlayListener(MediaPlayListener l) {
    mMediaPlayListener = l;
  }

  SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback() {
    public void surfaceChanged(SurfaceHolder holder, int format,
        int w, int h) {
      mSurfaceWidth = w;
      mSurfaceHeight = h;
      boolean isValidState = (mTargetState == STATE_PLAYING);
      boolean hasValidSize = (mVideoWidth == w && mVideoHeight == h);
      if (mMediaPlayer != null && isValidState && hasValidSize) {
        if (mSeekWhenPrepared != 0) {
          seekTo(mSeekWhenPrepared);
        }
        start();
      }
    }

    public void surfaceCreated(SurfaceHolder holder) {
      mSurfaceHolder = holder;
      openVideo();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
      // after we return from this we can't use the surface any more
      mSurfaceHolder = null;
      if (mMediaController != null) mMediaController.hide();
      release(true);
    }
  };

  /*
   * release the media player in any state
   */
  private void release(boolean cleartargetstate) {
    if (mMediaPlayer != null) {
      mMediaPlayer.reset();
      mMediaPlayer.release();
      mMediaPlayer = null;
      mCurrentState = STATE_IDLE;
      if (cleartargetstate) {
        mTargetState = STATE_IDLE;
      }
      AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
      am.abandonAudioFocus(null);
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    if (isInPlaybackState() && mMediaController != null) {
      toggleMediaControlsVisiblity();
    }
    return false;
  }

  @Override
  public boolean onTrackballEvent(MotionEvent ev) {
    if (isInPlaybackState() && mMediaController != null) {
      toggleMediaControlsVisiblity();
    }
    return false;
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK &&
        keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
        keyCode != KeyEvent.KEYCODE_VOLUME_DOWN &&
        keyCode != KeyEvent.KEYCODE_VOLUME_MUTE &&
        keyCode != KeyEvent.KEYCODE_MENU &&
        keyCode != KeyEvent.KEYCODE_CALL &&
        keyCode != KeyEvent.KEYCODE_ENDCALL;
    if (isInPlaybackState() && isKeyCodeSupported && mMediaController != null) {
      if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
          keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
        if (mMediaPlayer.isPlaying()) {
          pause();
          mMediaController.show(0);
        } else {
          start();
          mMediaController.hide();
        }
        return true;
      } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
        if (!mMediaPlayer.isPlaying()) {
          start();
          mMediaController.hide();
        }
        return true;
      } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
          || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
        if (mMediaPlayer.isPlaying()) {
          pause();
          mMediaController.show(0);
        }
        return true;
      } else {
        toggleMediaControlsVisiblity();
      }
    }

    return super.onKeyDown(keyCode, event);
  }

  private void toggleMediaControlsVisiblity() {
    if (mMediaController.isShowing()) {
      mMediaController.hide();
    } else {
      mMediaController.show(0);
    }
  }

  @Override
  public void start() {
    if (isInPlaybackState()) {
      mMediaPlayer.start();
      mCurrentState = STATE_PLAYING;
    }
    mTargetState = STATE_PLAYING;
    if (mMediaController != null) {
      mMediaController.show(0);
    }
  }

  @Override
  public void pause() {
    if (isInPlaybackState()) {
      if (mMediaPlayer.isPlaying()) {
        mMediaPlayer.pause();
        mCurrentState = STATE_PAUSED;
      }
    }
    mTargetState = STATE_PAUSED;
  }

  public void suspend() {
    release(false);
  }

  public void resume() {
    openVideo();
  }

  @Override
  public int getDuration() {
    if (isInPlaybackState()) {
      return mMediaPlayer.getDuration();
    }

    return -1;
  }

  @Override
  public int getCurrentPosition() {
    if (isInPlaybackState()) {
      return mMediaPlayer.getCurrentPosition();
    }
    return 0;
  }

  @Override
  public void seekTo(int msec) {
    if (isInPlaybackState()) {
      mMediaPlayer.seekTo(msec);
      mSeekWhenPrepared = 0;
    } else {
      mSeekWhenPrepared = msec;
    }
  }

  @Override
  public boolean isPlaying() {
    return isInPlaybackState() && mMediaPlayer.isPlaying();
  }

  @Override
  public int getBufferPercentage() {
    if (mMediaPlayer != null) {
      return mCurrentBufferPercentage;
    }
    return 0;
  }

  private boolean isInPlaybackState() {
    return (mMediaPlayer != null &&
        mCurrentState != STATE_ERROR &&
        mCurrentState != STATE_IDLE &&
        mCurrentState != STATE_PREPARING);
  }

  @Override
  public boolean canPause() {
    return mCanPause;
  }

  @Override
  public boolean canSeekBackward() {
    return mCanSeekBack;
  }

  @Override
  public boolean canSeekForward() {
    return mCanSeekForward;
  }

  @Override
  public int getAudioSessionId() {
    if (mAudioSession == 0) {
      MediaPlayer foo = new MediaPlayer();
      mAudioSession = foo.getAudioSessionId();
      foo.release();
    }
    return mAudioSession;
  }

  @Override
  public void stop() {
    stopPlayback();
    if (mMediaPlayListener != null) {
      mMediaPlayListener.onStop();
    }
  }

  public interface MediaPlayListener {
    /**
     * Called when the media file is ready for playback.
     *
     * @param mp the MediaPlayer that is ready for playback
     */
    void onPrepared(MediaPlayer mp);

    /**
     * Called when the end of a media source is reached during playback.
     *
     * @param mp the MediaPlayer that reached the end of the file
     */
    void onCompletion(MediaPlayer mp);

    /**
     * Called to indicate an error.
     *
     * @param mp      the MediaPlayer the error pertains to
     * @param what    the type of error that has occurred:
     * <ul>
     * <li>{@link MediaPlayer#MEDIA_ERROR_UNKNOWN}
     * <li>{@link MediaPlayer#MEDIA_ERROR_SERVER_DIED}
     * </ul>
     * @param extra an extra code, specific to the error. Typically
     * implementation dependent.
     * <ul>
     * <li>{@link MediaPlayer#MEDIA_ERROR_IO}
     * <li>{@link MediaPlayer#MEDIA_ERROR_MALFORMED}
     * <li>{@link MediaPlayer#MEDIA_ERROR_UNSUPPORTED}
     * <li>{@link MediaPlayer#MEDIA_ERROR_TIMED_OUT}
     * <li><code>MEDIA_ERROR_SYSTEM (-2147483648)</code> - low-level system error.
     * </ul>
     * @return True if the method handled the error, false if it didn't.
     * Returning false, or not having an OnErrorListener at all, will
     * cause the OnCompletionListener to be called.
     */
    boolean onError(MediaPlayer mp, int what, int extra);

    /**
     * Called to indicate an info or a warning.
     *
     * @param mp      the MediaPlayer the info pertains to.
     * @param what    the type of info or warning.
     * <ul>
     * <li>{@link MediaPlayer#MEDIA_INFO_UNKNOWN}
     * <li>{@link MediaPlayer#MEDIA_INFO_VIDEO_TRACK_LAGGING}
     * <li>{@link MediaPlayer#MEDIA_INFO_VIDEO_RENDERING_START}
     * <li>{@link MediaPlayer#MEDIA_INFO_BUFFERING_START}
     * <li>{@link MediaPlayer#MEDIA_INFO_BUFFERING_END}
     * <li><code>MEDIA_INFO_NETWORK_BANDWIDTH (703)</code> -
     *     bandwidth information is available (as <code>extra</code> kbps)
     * <li>{@link MediaPlayer#MEDIA_INFO_BAD_INTERLEAVING}
     * <li>{@link MediaPlayer#MEDIA_INFO_NOT_SEEKABLE}
     * <li>{@link MediaPlayer#MEDIA_INFO_METADATA_UPDATE}
     * <li>{@link MediaPlayer#MEDIA_INFO_UNSUPPORTED_SUBTITLE}
     * <li>{@link MediaPlayer#MEDIA_INFO_SUBTITLE_TIMED_OUT}
     * </ul>
     * @param extra an extra code, specific to the info. Typically
     * implementation dependent.
     * @return True if the method handled the info, false if it didn't.
     * Returning false, or not having an OnInfoListener at all, will
     * cause the info to be discarded.
     */
    boolean onInfo(MediaPlayer mp, int what, int extra);

    /**
     * Called when media player stop.
     */
    void onStop();
  }

}
