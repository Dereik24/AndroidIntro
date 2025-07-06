package com.example.safandfileprovider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;


public class AdbFileReceiver extends BroadcastReceiver {

    private static final String TAG = "AdbFileReceiver";
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String KEY_SELECTED_URI = "selectedUri";

    // This is the "action" string we will use in our ADB command.
    // It's like a unique channel name for our broadcast.
    public static final String ACTION_EXPORT_FILE = "com.example.safandfileprovider.action.EXPORT_FILE";

    @Override
    public void onReceive(Context context, Intent intent) {
        // This method is called when the receiver gets a broadcast that matches its intent filter.
        if (context != null && intent != null && ACTION_EXPORT_FILE.equals(intent.getAction())) {
            Log.d(TAG, "Received export file broadcast command.");

            // We show a Toast to give visual feedback that the command was received.
            Toast.makeText(context, "ADB command received!", Toast.LENGTH_SHORT).show();

            // The logic here is nearly identical to the exportDummyFile method in MainActivity.
            // We are re-using the same principles.
            exportFileFromCommand(context);
        }
    }

    private void exportFileFromCommand(Context context) {
        // 1. Get the saved folder URI from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uriString = prefs.getString(KEY_SELECTED_URI, null);

        if (uriString == null) {
            Log.e(TAG, "No folder URI saved. Cannot export file.");
            // Note: We can't show a Toast here reliably if the app isn't in the foreground. Logging is better.
            return;
        }

        Uri folderUri = Uri.parse(uriString);
        DocumentFile targetDirectory = DocumentFile.fromTreeUri(context, folderUri);

        if (targetDirectory == null || !targetDirectory.canWrite()) {
            Log.e(TAG, "Cannot write to the selected folder: " + uriString);
            return;
        }

        // In a real app, you would get the file to export from the intent's extras.
        // For now, we'll just create the same dummy file.
        String fileName = "data_from_adb.txt";
        String fileContents = "This file was created by an ADB command at " + System.currentTimeMillis();
        File internalFile = new File(context.getFilesDir(), fileName);

        try (FileOutputStream fos = new FileOutputStream(internalFile)) {
            fos.write(fileContents.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.e(TAG, "Failed to create internal file via ADB", e);
            return;
        }

        // NOTE: We are hardcoding the authority here because getting the BuildConfig
        // can be tricky in a BroadcastReceiver. This is a safe and common practice.
        String authority = "com.example.safandfileprovider.fileprovider";
        Uri internalFileUri = androidx.core.content.FileProvider.getUriForFile(context, authority, internalFile);

        DocumentFile newFile = targetDirectory.createFile("text/plain", fileName);
        if (newFile == null) {
            Log.e(TAG, "Could not create document file in target directory via ADB.");
            return;
        }

        try (InputStream in = context.getContentResolver().openInputStream(internalFileUri);
             OutputStream out = context.getContentResolver().openOutputStream(newFile.getUri())) {

            if (in == null || out == null) {
                throw new IOException("Failed to open streams for ADB copy");
            }

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            Log.d(TAG, "File exported successfully via ADB!");

        } catch (IOException e) {
            Log.e(TAG, "File copy failed via ADB", e);
        }
    }
}
