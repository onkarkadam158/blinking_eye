package com.example.app;




import android.app.Service;
import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.airbnb.lottie.LottieAnimationView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;

import pl.droidsonroids.gif.GifImageView;


public class OverlayService extends Service {

    private WindowManager windowManager;
    private View overlayView;
    private boolean isAltAnim = false;

    private final List<Object> gifAnimList = new ArrayList<>();
     private int currentGIFAnimIndex = 0;
    GifImageView gifView;

    private int[] GIFanimationList = {
            R.drawable.blink
    };

    private SharedPreferences prefs;
    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(this, R.style.Theme_App);
        LayoutInflater inflater = LayoutInflater.from(contextThemeWrapper);

        prefs = getSharedPreferences("overlay_prefs", MODE_PRIVATE);
        overlayView = inflater.inflate(R.layout.dialogue_layout, null);

        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        int screenHeight = metrics.heightPixels;

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // Overlay type
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,   // Make it non-focusable
                PixelFormat.TRANSLUCENT   // Allow transparency
        );
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        int defaultY = screenHeight - 300;  // Adjust this if needed
        int savedX = getSharedPreferences("overlay_prefs", MODE_PRIVATE).getInt("overlay_x", 0);
        int savedY = getSharedPreferences("overlay_prefs", MODE_PRIVATE).getInt("overlay_y", defaultY);

        layoutParams.x = savedX;
        layoutParams.y = savedY;

        prepareGIFList(getApplicationContext());
        gifView = overlayView.findViewById(R.id.animation_view);

        prepareGIFList(getApplicationContext());


        Object initialGif = gifAnimList.get(currentGIFAnimIndex);
        if (initialGif instanceof File) {
            gifView.setImageURI(Uri.fromFile((File) initialGif));
        } else if (initialGif instanceof Integer) {
            gifView.setImageResource((int) initialGif);
        }


        overlayView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private long touchStartTime;

            private static final int CLICK_THRESHOLD = 200;     // max duration (ms) to be a click
            private static final int MOVE_THRESHOLD = 10;       // max movement (px) to be a click

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) overlayView.getLayoutParams();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        touchStartTime = System.currentTimeMillis();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(overlayView, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        long clickDuration = System.currentTimeMillis() - touchStartTime;
                        float deltaX = Math.abs(event.getRawX() - initialTouchX);
                        float deltaY = Math.abs(event.getRawY() - initialTouchY);

                        if (clickDuration < CLICK_THRESHOLD && deltaX < MOVE_THRESHOLD && deltaY < MOVE_THRESHOLD) {
                            onOverlayClicked_forGIF();
                        }
                        return true;
                }

                return false;
            }
        });
        windowManager.addView(overlayView, layoutParams);
        overlayView.setVisibility(View.VISIBLE);
    }

    private void onOverlayClicked_forGIF() {
        // Rebuild list every time to reflect new uploads
        prepareGIFList(getApplicationContext());

        if (gifAnimList.isEmpty()) return;

        // Move to next GIF
        currentGIFAnimIndex = (currentGIFAnimIndex + 1) % gifAnimList.size();

        Object currentGif = gifAnimList.get(currentGIFAnimIndex);

        gifView.setImageDrawable(null); // reset to force refresh

        if (currentGif instanceof File) {
            gifView.setImageURI(Uri.fromFile((File) currentGif));
        } else if (currentGif instanceof Integer) {
            gifView.setImageResource((int) currentGif);
        }
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String packageName = null,status = null;

        if(intent!=null) {
            packageName=intent.getStringExtra("packageName");
            status=intent.getStringExtra("BackOrFore");
        }
        if (status != null && status.equals("1")) {
            if (!isAnyOverlayVisible()) {
                showTheOverlay(packageName);
            }
        } else if (status != null && status.equals("2")) {

            if (isAnyOverlayVisible()) {
                hideTheOverlay(packageName);
            }
        }
        return START_STICKY;
    }



    private void prepareGIFList(Context context) {
        List<Object> newGifList = new ArrayList<>();

        GifUploader gifUploader = new GifUploader(context, null);
        List<File> uploadedGifs = gifUploader.getAllUploadedGifs();

        if (uploadedGifs != null && !uploadedGifs.isEmpty()) {
            newGifList.addAll(uploadedGifs);
        } else {
            newGifList.add(R.drawable.blink);
        }

        // Preserve current index as long as possible
        if (currentGIFAnimIndex >= newGifList.size()) {
            currentGIFAnimIndex = 0;
        }

        gifAnimList.clear();
        gifAnimList.addAll(newGifList);
    }


    public boolean isAnyOverlayVisible() {
        return overlayView.getVisibility() == View.VISIBLE ;
    }
    public void showTheOverlay(String packageName) {
        overlayView.setVisibility(View.VISIBLE);
        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) overlayView.getLayoutParams();
        windowManager.updateViewLayout(overlayView, layoutParams);  // 🔧 Force update
    }
     public void hideTheOverlay(String packageName) {

        if (overlayView.getVisibility() == View.VISIBLE) {
            overlayView.setVisibility(View.GONE);
        }
        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) overlayView.getLayoutParams();
        windowManager.updateViewLayout(overlayView, layoutParams);
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null && windowManager != null) {
            windowManager.removeView(overlayView);
        }

    }




}
