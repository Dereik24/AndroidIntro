# SAF and FileProvider Demo App

This is a demonstration app for learning how to use the Storage Access Framework (SAF), FileProvider, and BroadcastReceivers in Android.

---

## ADB Commands

Use these commands from your computer's terminal to interact with the app. Ensure the app has been run at least once and a target folder has been selected.

### Export a Test File

This command tells the app to create a test file in its private storage and copy it to the selected public folder.

adb shell am broadcast -a com.example.safandfileprovider.action.EXPORT_FILE -n com.example.safandfileprovider/.AdbFileReceiver

### Export a Test File

These command tells push a file to Documents in open storage on the device. And then tell the app to grab that individual file and copy it into its internal space.

# (Replace 'import_test.txt' with your file and 'Documents' with your chosen folder)
adb push import_test.txt /sdcard/Documents/

adb shell am broadcast -a com.example.safandfileprovider.action.IMPORT_FILE -n com.example.safandfileprovider/.AdbFileReceiver --es com.example.safandfileprovider.extra.FILENAME "import_test.txt"



