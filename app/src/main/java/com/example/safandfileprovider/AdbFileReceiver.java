package com.example.safandfileprovider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class AdbFileReceiver extends BroadcastReceiver {

    private static final String TAG = "AdbFileReceiver";
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String KEY_SELECTED_URI = "selectedUri";

    public static final String ACTION_EXPORT_FILE = "com.example.safandfileprovider.action.EXPORT_FILE";
    // --- NEW: Action and Extra for the import command ---
    public static final String ACTION_IMPORT_FILE = "com.example.safandfileprovider.action.IMPORT_FILE";
    public static final String EXTRA_FILENAME = "com.example.safandfileprovider.extra.FILENAME";


    @Override
    public void onReceive(Context context, Intent intent) {
        // --- MODIFIED: This method now acts as a switchboard ---
        if (context == null || intent == null || intent.getAction() == null) {
            return;
        }

        // Check which action was received and call the appropriate method.
        String action = intent.getAction();
        Log.d(TAG, "Received broadcast with action: " + action);

        if (ACTION_EXPORT_FILE.equals(action)) {
            Toast.makeText(context, "Export command received!", Toast.LENGTH_LONG).show();
            exportFileFromCommand(context);
        } else if (ACTION_IMPORT_FILE.equals(action)) {
            // --- NEW: Handle the import action ---
            Toast.makeText(context, "Import command received!", Toast.LENGTH_LONG).show();
            // Get the filename to import from the intent's "extra" data.
            String filename = intent.getStringExtra(EXTRA_FILENAME);
            if (filename == null || filename.isEmpty()) {
                Log.e(TAG, "Import command received without a filename.");
                return;
            }
            importFileFromCommand(context, filename);
        }
    }

    private void exportFileFromCommand(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uriString = prefs.getString(KEY_SELECTED_URI, null);

        if (uriString == null) {
            Log.e(TAG, "No folder URI saved. Cannot export file.");
            return;
        }

        Uri folderUri = Uri.parse(uriString);
        DocumentFile targetDirectory = DocumentFile.fromTreeUri(context, folderUri);

        if (targetDirectory == null || !targetDirectory.canWrite()) {
            Log.e(TAG, "Cannot write to the selected folder: " + uriString);
            return;
        }

        String fileName = "data_from_adb.txt";
        String fileContents = "This file was created by an ADB command at " + System.currentTimeMillis();
        File internalFile = new File(context.getFilesDir(), fileName);

        try (FileOutputStream fos = new FileOutputStream(internalFile)) {
            fos.write(fileContents.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.e(TAG, "Failed to create internal file via ADB", e);
            return;
        }

        String authority = "com.example.safandfileprovider.fileprovider";
        Uri internalFileUri = FileProvider.getUriForFile(context, authority, internalFile);

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


    private void importFileFromCommand(Context context, String filename) {
        // 1. Get the saved folder URI from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uriString = prefs.getString(KEY_SELECTED_URI, null);

        if (uriString == null) {
            Log.e(TAG, "No folder URI saved. Cannot import file.");
            return;
        }

        // 2. Find the source file in the public directory
        Uri folderUri = Uri.parse(uriString);
        DocumentFile sourceDirectory = DocumentFile.fromTreeUri(context, folderUri);
        if (sourceDirectory == null) {
            Log.e(TAG, "Could not access source directory: " + uriString);
            return;
        }

        DocumentFile sourceFile = sourceDirectory.findFile(filename);
        if (sourceFile == null || !sourceFile.exists() || !sourceFile.canRead()) {
            Log.e(TAG, "Source file not found or cannot be read: " + filename);
            return;
        }

        // 3. Define the destination file in our app's private internal storage
        File destinationFile = new File(context.getFilesDir(), filename);

        // 4. Copy the file contents
        try (InputStream in = context.getContentResolver().openInputStream(sourceFile.getUri());
             OutputStream out = new FileOutputStream(destinationFile)) {

            if (in == null) {
                throw new IOException("Failed to open input stream for source file.");
            }

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            Log.d(TAG, "File '" + filename + "' imported successfully into internal storage!");

        } catch (IOException e) {
            Log.e(TAG, "File import failed for: " + filename, e);
        }
    }
}
