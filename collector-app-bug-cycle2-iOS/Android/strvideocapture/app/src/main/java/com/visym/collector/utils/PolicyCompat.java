package com.visym.collector.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.view.Window;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class PolicyCompat {
  private static final String PHONE_WINDOW_CLASS_NAME = "com.android.internal.policy.PhoneWindow";
  private static final String POLICY_MANAGER_CLASS_NAME = "com.android.internal.policy.PolicyManager";


  private PolicyCompat() {
  }


  /*
   * Private methods
   */
  private static Window createPhoneWindow(Context context) {
    try {
      /* Find class */
      Class<?> cls = Class.forName(PHONE_WINDOW_CLASS_NAME);

      /* Get constructor */
      Constructor c = cls.getConstructor(Context.class);

      /* Create instance */
      return (Window) c.newInstance(context);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(PHONE_WINDOW_CLASS_NAME + " could not be loaded", e);
    } catch (Exception e) {
      throw new RuntimeException(PHONE_WINDOW_CLASS_NAME + " class could not be instantiated", e);
    }
  }

  private static Window makeNewWindow(Context context) {
    try {
      /* Find class */
      Class<?> cls = Class.forName(POLICY_MANAGER_CLASS_NAME);

      /* Find method */
      Method m = cls.getMethod("makeNewWindow", Context.class);

      /* Invoke method */
      return (Window) m.invoke(null, context);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(POLICY_MANAGER_CLASS_NAME + " could not be loaded", e);
    } catch (Exception e) {
      throw new RuntimeException(POLICY_MANAGER_CLASS_NAME + ".makeNewWindow could not be invoked", e);
    }
  }

  /*
   * Public methods
   */
  public static Window createWindow(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
      return createPhoneWindow(context);
    else
      return makeNewWindow(context);
  }

  public static MediaPlayer getMediaPlayer(Context context) {

    MediaPlayer mediaplayer = new MediaPlayer();

    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
      return mediaplayer;
    }

    try {
      Class<?> cMediaTimeProvider = Class.forName("android.media.MediaTimeProvider");
      Class<?> cSubtitleController = Class.forName("android.media.SubtitleController");
      Class<?> iSubtitleControllerAnchor = Class.forName("android.media.SubtitleController$Anchor");
      Class<?> iSubtitleControllerListener = Class.forName("android.media.SubtitleController$Listener");

      Constructor constructor = cSubtitleController.getConstructor(Context.class,
          cMediaTimeProvider, iSubtitleControllerListener);

      Object subtitleInstance = constructor.newInstance(context, null, null);

      Field f = cSubtitleController.getDeclaredField("mHandler");

      f.setAccessible(true);
      try {
        f.set(subtitleInstance, new Handler());
      } catch (IllegalAccessException e) {return mediaplayer;} finally {
        f.setAccessible(false);
      }

      Method setSubtitleAnchor = mediaplayer.getClass().getMethod("setSubtitleAnchor", cSubtitleController,
          iSubtitleControllerAnchor);

      setSubtitleAnchor.invoke(mediaplayer, subtitleInstance, null);
      //Log.e("", "subtitle is setted :p");
    } catch (Exception ignored) {}

    return mediaplayer;
  }
}
