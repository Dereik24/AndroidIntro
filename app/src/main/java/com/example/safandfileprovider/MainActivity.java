package com.example.safandfileprovider; // Make sure this matches your project's package name

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.safandfileprovider.BuildConfig;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String KEY_SELECTED_URI = "selectedUri";
    private static final String TAG = "MainActivity";
    private Button mySelectFolderButton;
    private Button myExportButton;
    private ActivityResultLauncher<Uri> openDirectoryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        myExportButton = findViewById(R.id.exportButton);
        mySelectFolderButton = findViewById(R.id.selectFolderButton);

        openDirectoryLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(),
                uri -> {
                    if (uri != null) {

                        //Getting persistable URI permissions (URI lasts as long as app)
                        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                        getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        //Saving this permission'ed URI to SharedPreferences
                        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                        editor.putString(KEY_SELECTED_URI, uri.toString());
                        editor.apply();

                        Toast.makeText(this, "Folder selection saved",Toast.LENGTH_LONG).show();

                    } else {
                        Toast.makeText(this, "No folder selected", Toast.LENGTH_SHORT).show();
                    }
                });

        mySelectFolderButton.setOnClickListener(v -> openDirectoryLauncher.launch(null));

        myExportButton.setOnClickListener(v -> exportDummyFile());
    }

    private void exportDummyFile() {
        // 1. Get the saved folder URI from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String uriString = prefs.getString(KEY_SELECTED_URI, null);

        if (uriString == null) {
            Toast.makeText(this, "Please select a folder first.", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri folderUri = Uri.parse(uriString);
        DocumentFile targetDirectory = DocumentFile.fromTreeUri(this, folderUri);

        if (targetDirectory == null || !targetDirectory.canWrite()) {
            Toast.makeText(this, "Cannot write to the selected folder.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Cannot write to target directory: " + uriString);
            return;
        }

        // 2. Create the dummy file in internal storage
        String fileName = "dummy_data.txt";
        String fileContents = "Hello from FileProvider! This is a test.";
        File internalFile = new File(getFilesDir(), fileName);

        try (FileOutputStream fos = new FileOutputStream(internalFile)) {
            fos.write(fileContents.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.e(TAG, "Failed to create internal file", e);
            Toast.makeText(this, "Error creating internal file.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Get a secure URI for the internal file using FileProvider
        // The authority must match what you declared in AndroidManifest.xml
        Uri internalFileUri = FileProvider.getUriForFile(this,
                BuildConfig.APPLICATION_ID + ".fileprovider", internalFile);

        // 4. Create the destination file in the public folder and copy the content
        DocumentFile newFile = targetDirectory.createFile("text/plain", fileName);
        if (newFile == null) {
            Toast.makeText(this, "Failed to create destination file.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Could not create document file in target directory.");
            return;
        }

        try (InputStream in = getContentResolver().openInputStream(internalFileUri);
             OutputStream out = getContentResolver().openOutputStream(newFile.getUri())) {

            if (in == null || out == null) {
                throw new IOException("Failed to open streams");
            }

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            Toast.makeText(this, "File exported successfully!", Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Log.e(TAG, "File copy failed", e);
            Toast.makeText(this, "File copy failed.", Toast.LENGTH_SHORT).show();
        }
    }
}
