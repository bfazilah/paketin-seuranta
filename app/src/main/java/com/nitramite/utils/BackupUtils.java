package com.nitramite.utils;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.nitramite.paketinseuranta.R;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import static com.nitramite.paketinseuranta.Constants.DATABASE_NAME;

public class BackupUtils {

    @NonNls
    private static final String TAG = "BackupUtils";

    // Backup database
    public static Backup backupDatabase(Context context) {
        Backup backup = new Backup();
        try {
            FileInputStream dbFileInputStream = getDBFileInputStream(context, backup);
            OutputStream dbFileOutputStream = getDBOutputStream(context, backup);

            // Write new file out
            byte[] buffer = new byte[1024];
            int length;
            while ((length = dbFileInputStream.read(buffer)) > 0) {
                dbFileOutputStream.write(buffer, 0, length);
            }
            dbFileOutputStream.flush();
            dbFileOutputStream.close();
            dbFileInputStream.close();

            backup.setSuccess(true);
            return backup;
        } catch (IOException e) {
            return backup;
        }
    }


    // Restore database
    public static Backup restoreDatabase(Context context) {
        Backup backup = new Backup();
        try {
            File targetPathFile = getCleanedAppDBTargetPath(context);
            FileInputStream fileInputStream = getDBFileInputSteamFromExternalStorage(context);

            Log.i(TAG, targetPathFile.getPath());

            OutputStream output = new FileOutputStream(targetPathFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fileInputStream.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }

            // Flush & Close
            output.flush();
            output.close();
            fileInputStream.close();

            backup.setSuccess(true);
            return backup;
        } catch (IOException e) {
            Log.i(TAG, e.toString());
            return backup;
        }
    }


    /**
     * Get database file input stream object
     *
     * @param context Context
     * @return file input stream
     * @throws FileNotFoundException database file not found
     */
    private static FileInputStream getDBFileInputStream(Context context, Backup backup) throws FileNotFoundException {
        @SuppressLint("SdCardPath") final String databaseLocation = "/data/data/" + context.getApplicationContext().getPackageName() + "/databases/" + DATABASE_NAME;
        File databaseFile = new File(databaseLocation);
        backup.setFileName(DATABASE_NAME);
        return new FileInputStream(databaseFile);
    }


    /**
     * Get database file output stream object
     *
     * @param context Context
     * @return file output stream
     * @throws FileNotFoundException todo: document uri not found?
     */
    private static OutputStream getDBOutputStream(Context context, Backup backup) throws FileNotFoundException, NullPointerException {
        OutputStream outputStream = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, DATABASE_NAME);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/x-sqlite3");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri documentUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
            outputStream = resolver.openOutputStream(Objects.requireNonNull(documentUri));
            backup.setLocation(context.getString(R.string.backup_location_external_storage_downloads_dir));
        } else {
            @SuppressWarnings("deprecation") String downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
            File file = new File(downloadsDir, DATABASE_NAME);
            outputStream = new FileOutputStream(file);
            backup.setLocation(context.getString(R.string.backup_location_external_storage_downloads_dir));
        }
        return outputStream;
    }


    /**
     * Get file input stream from db source path
     *
     * @param context Context
     * @return file input steam
     * @throws FileNotFoundException db file not found
     */
    private static FileInputStream getDBFileInputSteamFromExternalStorage(Context context) throws FileNotFoundException, NullPointerException {
        FileInputStream fileInputStream = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            // TODO: Wrong implementation because Google's documentation sucks with this subject.
            // TODO: currently using android:requestLegacyExternalStorage="true" at manifest.
            // TODO: research proper solution!
            @SuppressWarnings("deprecation") String downloadsDir = Environment.getExternalStorageDirectory() + "/Download/";
            File file = new File(downloadsDir, DATABASE_NAME);
            fileInputStream = new FileInputStream(file);

        } else {
            @SuppressWarnings("deprecation") String downloadsDir = Environment.getExternalStorageDirectory() + "/Download/";
            File file = new File(downloadsDir, DATABASE_NAME);
            fileInputStream = new FileInputStream(file);

        }
        return fileInputStream;
    }


    /**
     * Get file object of target db restore path, also cleans destination from old files
     *
     * @param context Context
     * @return file object
     */
    private static File getCleanedAppDBTargetPath(Context context) {
        @SuppressLint("SdCardPath") final String dbOnAppPath = "/data/data/" + context.getApplicationContext().getPackageName() + "/databases/";
        File file = new File(dbOnAppPath + DATABASE_NAME);
        if (file.exists()) {
            file.delete(); // Clean existing junk first from application
        }
        File dbShmFile = new File(dbOnAppPath + DATABASE_NAME + "-shm");
        if (dbShmFile.exists()) {
            dbShmFile.delete();
        }
        File dbWalFile = new File(dbOnAppPath + DATABASE_NAME + "-wal");
        if (dbWalFile.exists()) {
            dbWalFile.delete();
        }
        return file;
    }


}
