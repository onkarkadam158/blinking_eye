package com.example.app;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.appcompat.app.AppCompatActivity;


import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_OVERLAY_PERMISSION = 1001;

    private GifUploader gifUploader;
    private ImageView gifImageView;

    ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = findViewById(R.id.buttonRequestPermissions);
        Button uploadButton = findViewById(R.id.uploadButton);
        RecyclerView gifRecyclerView = findViewById(R.id.gifRecyclerView);
        gifRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 👇 Initialize uploader before using it
        gifUploader = new GifUploader(this, new GifUploader.UploadCallback() {
            @Override
            public void onUploadSuccess(File gifFile) {
                Toast.makeText(MainActivity.this, "GIF uploaded!", Toast.LENGTH_SHORT).show();
                loadAndDisplayAllGifs(gifRecyclerView);
            }

            @Override
            public void onUploadFailure(String errorMessage) {
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        // ✅ Now it's safe to load the list
        loadAndDisplayAllGifs(gifRecyclerView);

        uploadButton.setOnClickListener(v -> gifUploader.startGifPicker(this));
        button.setOnClickListener(v -> requestNecessaryPermissions());
    }


    private void loadAndDisplayAllGifs(RecyclerView recyclerView) {
        List<File> gifFiles = gifUploader.getAllUploadedGifs();

        GifAdapter adapter = new GifAdapter(
                this,
                gifFiles,
                gifUploader,
                () -> {} // You can refresh the list or trigger something on delete
        );
        recyclerView.setAdapter(adapter);
    }

    private void requestNecessaryPermissions() {
        // Request overlay permission
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }

        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
            return;
        }

        if (!hasUsageAccessPermission()) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }


        checkAndStartForegroundService();
    }

    private boolean hasUsageAccessPermission() {
        try {
            AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), 0);
            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    appInfo.uid, appInfo.packageName);
            return (mode == AppOpsManager.MODE_ALLOWED);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            requestNecessaryPermissions();
        }
        super.onActivityResult(requestCode, resultCode, data);

        if (gifUploader != null) {
            gifUploader.handleActivityResult(requestCode, resultCode, data);
        }
    }

    private void checkAndStartForegroundService() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        == android.content.pm.PackageManager.PERMISSION_GRANTED) {

            if (Settings.canDrawOverlays(this)) {
                startAppLaunchMonitoringInNotification();
                Toast.makeText(this, "Foreground service started!", Toast.LENGTH_SHORT).show();

            }
        }
    }
    private void startAppLaunchMonitoringInNotification() {
        Intent serviceIntent1 = new Intent(getApplicationContext(), NotificationForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent1);
        } else {
            startService(serviceIntent1);
        }
    }
}
