package com.replay;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class FloatingModule extends ReactContextBaseJavaModule {
  private final ReactApplicationContext context;

  public FloatingModule(ReactApplicationContext ctx) {
    super(ctx);
    context = ctx;
  }

  @Override
  public String getName() {
    return "FloatingModule";
  }

  @ReactMethod
  public void start(Promise promise) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
      promise.reject("overlay_permission_missing", "Enable Display over other apps first");
      return;
    }

    Intent intent = new Intent(context, FloatingService.class);
    context.startService(intent);
    promise.resolve(true);
  }

  @ReactMethod
  public void stop(Promise promise) {
    Intent intent = new Intent(context, FloatingService.class);
    context.stopService(intent);
    promise.resolve(true);
  }

  @ReactMethod
  public void requestPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)) {
      return;
    }

    Intent intent = new Intent(
      Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
      Uri.parse("package:" + context.getPackageName())
    );
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(intent);
  }
}
