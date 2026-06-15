package com.replay;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class FloatingService extends Service {
  private WindowManager windowManager;
  private View floatingView;

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    super.onCreate();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
      Toast.makeText(this, "Enable Display over other apps first", Toast.LENGTH_SHORT).show();
      stopSelf();
      return;
    }

    windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

    Button button = new Button(this);
    button.setText("REC");
    button.setTextColor(Color.WHITE);
    button.setTextSize(12);
    button.setBackgroundColor(Color.rgb(37, 99, 235));
    button.setOnClickListener(v ->
      Toast.makeText(this, "Replay capture tapped", Toast.LENGTH_SHORT).show()
    );

    int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
      ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
      : WindowManager.LayoutParams.TYPE_PHONE;

    WindowManager.LayoutParams params = new WindowManager.LayoutParams(
      200,
      200,
      overlayType,
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
      PixelFormat.TRANSLUCENT
    );

    params.gravity = Gravity.TOP | Gravity.START;
    params.x = 100;
    params.y = 300;

    button.setOnTouchListener(new View.OnTouchListener() {
      private int initialX;
      private int initialY;
      private float initialTouchX;
      private float initialTouchY;

      @Override
      public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
          case MotionEvent.ACTION_DOWN:
            initialX = params.x;
            initialY = params.y;
            initialTouchX = event.getRawX();
            initialTouchY = event.getRawY();
            return false;
          case MotionEvent.ACTION_MOVE:
            params.x = initialX + (int) (event.getRawX() - initialTouchX);
            params.y = initialY + (int) (event.getRawY() - initialTouchY);
            if (floatingView != null) {
              windowManager.updateViewLayout(floatingView, params);
            }
            return true;
          default:
            return false;
        }
      }
    });

    floatingView = button;
    windowManager.addView(floatingView, params);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (floatingView != null && windowManager != null) {
      windowManager.removeView(floatingView);
      floatingView = null;
    }
  }
}
