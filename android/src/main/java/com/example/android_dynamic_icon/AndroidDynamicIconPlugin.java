package com.example.android_dynamic_icon;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import android.app.Activity;
import android.content.Context;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.embedding.engine.plugins.lifecycle.FlutterLifecycleAdapter;
import io.flutter.plugin.common.BinaryMessenger;
import android.app.Application;
import android.os.Bundle;
import androidx.annotation.VisibleForTesting;
import io.flutter.Log;

/** AndroidDynamicIconPlugin */
public class AndroidDynamicIconPlugin implements FlutterPlugin, ActivityAware {
  private MethodChannel channel;
  private MethodCallImplementation handler;
  private static final String TAG = "[android_dynamic_icon]";
  private static final String CHANNEL_ID = "AndroidDynamicIcon";

  public static String getTAG() {
    return TAG;
  }

  public static String getChannelId() {
    return CHANNEL_ID;
  }

  private void setupChannel(BinaryMessenger messenger, Context context, Activity activity) {
    channel = new MethodChannel(messenger, CHANNEL_ID);
    handler = new MethodCallImplementation(context, activity);
    channel.setMethodCallHandler(handler);
  }

  private void teardownChannel() {
    channel.setMethodCallHandler(null);
    channel = null;
    handler = null;
  }

  private class LifeCycleObserver implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private final Activity thisActivity;

    LifeCycleObserver(Activity activity) {
      this.thisActivity = activity;
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {}

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {}

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {}

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
      Log.i("ChangeIcon", "The app has paused");
      handler.updateIcon();
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {}

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {}

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityResumed(Activity activity) {}

    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {
      if (thisActivity == activity && activity.getApplicationContext() != null) {
        ((Application) activity.getApplicationContext()).unregisterActivityLifecycleCallbacks(this);
      }
    }

    @Override
    public void onActivityStopped(Activity activity) {
      if (thisActivity == activity) {}
    }
  }

  private class ActivityState {
    private Application application;
    private Activity activity;
    private LifeCycleObserver observer;
    private ActivityPluginBinding activityBinding;
    private Lifecycle lifecycle;

    ActivityState(final Application application, final Activity activity, final BinaryMessenger messenger, final ActivityPluginBinding activityBinding) {
      this.application = application;
      this.activity = activity;
      this.activityBinding = activityBinding;
      observer = new LifeCycleObserver(activity);
      lifecycle = FlutterLifecycleAdapter.getActivityLifecycle(activityBinding);
      lifecycle.addObserver(observer);
    }

    void release() {
      if (activityBinding != null) {
        activityBinding = null;
      }
      if (lifecycle != null) {
        lifecycle.removeObserver(observer);
        lifecycle = null;
      }
      if (application != null) {
        application.unregisterActivityLifecycleCallbacks(observer);
        application = null;
      }
      activity = null;
      observer = null;
    }

    Activity getActivity() {
      return activity;
    }
  }

  private FlutterPluginBinding pluginBinding;
  private ActivityState activityState;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    setupChannel(flutterPluginBinding.getBinaryMessenger(), flutterPluginBinding.getApplicationContext(), null);
    pluginBinding = flutterPluginBinding;
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    setup(pluginBinding.getBinaryMessenger(), (Application) pluginBinding.getApplicationContext(), binding.getActivity(), binding);
    handler.setActivity(binding.getActivity());
  }

  @VisibleForTesting
  final ActivityState getActivityState() {
    return activityState;
  }

  private void setup(final BinaryMessenger messenger, final Application application, final Activity activity, final ActivityPluginBinding activityBinding) {
    activityState = new ActivityState(application, activity, messenger, activityBinding);
  }

  private void tearDown() {
    if (activityState != null) {
      activityState.release();
      activityState = null;
    }
  }

  @Override
  public void onDetachedFromActivity() {
    tearDown();
    handler.setActivity(null);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    onDetachedFromActivity();
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    onAttachedToActivity(binding);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    teardownChannel();
  }
}
