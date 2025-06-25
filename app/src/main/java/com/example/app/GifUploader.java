package com.example.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class GifUploader {
    private static final int PICK_GIF_REQUEST_CODE = 1001;
    private static final String GIF_DIRECTORY_NAME = "uploaded_gifs";

    private final Context context;
    private final UploadCallback callback;

    public interface UploadCallback {
        void onUploadSuccess(File gifFile);
        void onUploadFailure(String errorMessage);
    }

    public GifUploader(Context context, UploadCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    // ========= 1. Pick GIF from Storage ============
    public void startGifPicker(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/gif");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        activity.startActivityForResult(Intent.createChooser(intent, "Select a GIF"), PICK_GIF_REQUEST_CODE);
    }

    // ========= 2. Handle Result and Save ============
    public void handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d("GifUploader", "handleActivityResult called");

        if (requestCode == PICK_GIF_REQUEST_CODE) {
            Log.d("GifUploader", "Request code matched");

            if (resultCode == Activity.RESULT_OK && data != null) {
                Uri gifUri = data.getData();
                Log.d("GifUploader", "GIF URI: " + gifUri);

                if (gifUri != null) {
                    try {
                        String fileName = getFileNameFromUri(gifUri);
                        Log.d("GifUploader", "File name: " + fileName);

                        File directory = getGifStorageDirectory();
                        if (!directory.exists()) {
                            boolean created = directory.mkdirs();
                            Log.d("GifUploader", "Created directory: " + created);
                        }

                        File destinationFile = new File(directory, fileName);
                        copyToInternalStorage(gifUri, destinationFile);

                        Log.d("GifUploader", "File saved to: " + destinationFile.getAbsolutePath());

                        // This should now be triggered
                        callback.onUploadSuccess(destinationFile);

                    } catch (Exception e) {
                        Log.e("GifUploader", "Error saving GIF", e);
                        callback.onUploadFailure("Error saving GIF: " + e.getMessage());
                    }
                } else {
                    Log.e("GifUploader", "GIF URI is null");
                    callback.onUploadFailure("GIF URI is null");
                }
            } else {
                Log.w("GifUploader", "Result canceled or data is null");
            }
        }
    }


    // ========= 3. Get All Uploaded GIF Files ============
    public List<File> getAllUploadedGifs() {
        List<File> gifFiles = new ArrayList<>();
        File dir = getGifStorageDirectory();
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((file) -> file.getName().endsWith(".gif"));
            if (files != null) {
                for (File f : files) {
                    gifFiles.add(f);
                }
            }
        }
        return gifFiles;
    }

    // ========= 4. Delete a GIF File ============
    public boolean deleteGif(File gifFile) {
        return gifFile.exists() && gifFile.delete();
    }

    // ========= 5. Return List of File Paths or IDs for UI ============
    public List<String> getUploadedGifPaths() {
        List<String> paths = new ArrayList<>();
        for (File gif : getAllUploadedGifs()) {
            paths.add(gif.getAbsolutePath());
        }
        return paths;
    }

    // ========= Internal Helpers ============

    private File getGifStorageDirectory() {
        return new File(context.getFilesDir(), GIF_DIRECTORY_NAME);
    }

    private String getFileNameFromUri(Uri uri) {
        String result = "uploaded_" + System.currentTimeMillis() + ".gif";
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (index != -1) {
                            result = cursor.getString(index);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } else if ("file".equals(uri.getScheme())) {
            result = new File(uri.getPath()).getName();
        }
        return result;
    }

    private void copyToInternalStorage(Uri uri, File destination) throws Exception {
        InputStream input = context.getContentResolver().openInputStream(uri);
        OutputStream output = new FileOutputStream(destination);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) > 0) {
            output.write(buffer, 0, bytesRead);
        }
        input.close();
        output.close();
    }
}
