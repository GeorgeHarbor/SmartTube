package com.liskovsoft.smartyoutubetv2.common.misc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build.VERSION;
import android.provider.OpenableColumns;

import com.liskovsoft.sharedutils.helpers.DateHelper;
import com.liskovsoft.sharedutils.helpers.FileHelpers;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.BackupSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager.OnError;
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity.OnResult;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class BackupAndRestoreHelper implements OnResult {
    public static final String BACKUP_FOLDER_NAME = "SmartTubeBackup";
    private static final int REQ_PICK_FILES = 1001;
    private final Context mContext;
    private Runnable mOnSuccess;
    private final String[] mPreferredFileManagers = {
            "com.ghisler.android.TotalCommander",
            "com.lonelycatgames.Xplore",
            "com.alphainventor.filemanager",
            "pl.solidexplorer2"
    };

    public BackupAndRestoreHelper(Context context) {
        mContext = context;
    }

    public void exportAppMediaFolder() {
        File mediaDir = FileHelpers.getExternalMediaDirectory(mContext);
        File dataDir = new File(mediaDir, "data");
        if (!dataDir.exists() || FileHelpers.isEmpty(dataDir) || VERSION.SDK_INT < 29) return;

        String backupZipName = getSavedBackupZipName();

        MediaStoreFile file = new MediaStoreFile(mContext, backupZipName, BACKUP_FOLDER_NAME);
        if (!file.isWritable()) {
            backupZipName = createBackupZipNameWithTimestamp();
            getGeneralData().setBackupZipName(backupZipName);
            file = new MediaStoreFile(mContext, backupZipName, BACKUP_FOLDER_NAME);
        }

        if (!file.isWritable()) {
            deleteTimeStamp(); // User copied full old media directory (with the old timestamp)
            backupZipName = createBackupZipNameWithTimestamp();
            getGeneralData().setBackupZipName(backupZipName);
            file = new MediaStoreFile(mContext, backupZipName, BACKUP_FOLDER_NAME);
        }

        if (file.isWritable()) {
            final File zipFile = new File(mediaDir, backupZipName);
            ZipHelper2.zipDirectory(dataDir, zipFile);

            if (zipFile.exists()) {
                file.copyFrom(zipFile);
                // Delete temporary zip
                zipFile.delete();
            }
        }

        //Uri uri = FileProvider.getUriForFile(
        //        mContext,
        //        mContext.getPackageName() + ".update_provider",
        //        zipFile
        //);
        //
        //try {
        //    openFileManager(uri);
        //} catch (Exception e) {
        //    // Activity launch may fail if called from background (e.g. WorkManager)
        //    e.printStackTrace();
        //}
    }

    private void openFileManager(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        //intent.setType("application/zip");
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PackageManager pm = mContext.getPackageManager();

        for (String pkg : mPreferredFileManagers) {
            Intent targeted = new Intent(intent);
            targeted.setPackage(pkg);
            if (targeted.resolveActivity(pm) != null) {
                mContext.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                mContext.startActivity(targeted);
                return;
            }
        }

        mContext.startActivity(Intent.createChooser(intent, mContext.getString(R.string.app_backup)));
    }

    /**
     * NOTE: The file picker relies on apps that support the Storage Access Framework (SAF).
     * At the moment, no known third-party file manager properly supports selecting ZIP
     * archives through this API, so the backup file may not appear in the picker.
     */
    public void importAppMediaFolder(Runnable onSuccess) {
        if (VERSION.SDK_INT < 19 || onSuccess == null) {
            return;
        }

        mOnSuccess = onSuccess;

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        //intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        //intent.addCategory(Intent.CATEGORY_OPENABLE);
        //intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
        //        "application/zip",
        //        "application/x-zip-compressed"
        //});

        ((MotherActivity) mContext).addOnResult(this);

        ((Activity) mContext).startActivityForResult(intent, REQ_PICK_FILES);
    }

    @Override
    public void onResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_PICK_FILES && resultCode == Activity.RESULT_OK) {
            if (data == null) return;

            Uri uri = data.getData();
            if (uri == null && data.getClipData() != null) {
                uri = data.getClipData().getItemAt(0).getUri();
            }

            unpackTempZip(uri, () -> mOnSuccess.run(), null);
        }
    }

    public void handleIncomingZip(Intent intent) {
        if (intent == null) {
            return;
        }

        Uri zipUri = null;

        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            zipUri = intent.getData();
        } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
            zipUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }

        final Uri selected=zipUri;
        new android.app.AlertDialog.Builder(mContext)
            .setTitle("Import settings backup?")
            .setMessage("Only approved settings are imported. Accounts, passwords and casting identities are excluded.")
            .setNegativeButton(android.R.string.cancel,(dialog,which)->{if(mContext instanceof android.app.Activity)((android.app.Activity)mContext).finish();})
            .setPositiveButton(android.R.string.ok,(dialog,which)->unpackTempZip(selected,
                ()->BackupSettingsPresenter.instance(mContext).showLocalRestoreDialogApi30(),
                error->MessageHelpers.showLongMessage(mContext,"Invalid settings backup"))).show();
    }

    public void unpackTempZip(File tempZip) {
        if (!tempZip.exists()) {
            return;
        }

        // Target folder: /Android/media/<package>/data
        File mediaDir = FileHelpers.getExternalMediaDirectory(mContext);
        File dataDir = new File(mediaDir, "data");

        File staging=new File(mContext.getCacheDir(),"import-"+java.util.UUID.randomUUID());
        File previous=new File(mediaDir,".previous-settings");
        try {
            SafeZip.extract(tempZip,staging);
            SettingsBackup.sanitizeTree(staging);
            File incoming=new File(staging,"data").exists()?new File(staging,"data"):staging;
            File prepared=new File(mediaDir,".prepared-settings");
            SafeZip.delete(prepared);
            FileHelpers.copy(incoming,prepared);
            SafeZip.delete(previous);
            if(dataDir.exists() && !dataDir.renameTo(previous))throw new java.io.IOException("Cannot preserve existing settings");
            if(!prepared.renameTo(dataDir)){
                previous.renameTo(dataDir);
                throw new java.io.IOException("Cannot stage settings");
            }
            SafeZip.delete(previous);
        }catch(java.io.IOException failure){throw new IllegalStateException("Invalid settings archive",failure);}
        finally{SafeZip.delete(staging);tempZip.delete();}

        // Delete the temporary ZIP
        tempZip.delete();
    }

    private static final java.util.concurrent.ThreadPoolExecutor IMPORTS = new java.util.concurrent.ThreadPoolExecutor(
        1,1,0,java.util.concurrent.TimeUnit.MILLISECONDS,new java.util.concurrent.ArrayBlockingQueue<>(2));
    private void unpackTempZip(Uri zipUri, Runnable onSuccess, OnError onError) {
        android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
        try {
            IMPORTS.execute(()->{
                try {
                    if(zipUri==null)throw new IllegalArgumentException("No backup selected");
                    unpackTempZip(zipUri);
                    if(onSuccess!=null)main.post(onSuccess);
                }catch(Exception failure){
                    main.post(()->{
                        if(onError!=null)onError.onError(failure);
                        else MessageHelpers.showLongMessage(mContext,"Invalid settings backup");
                    });
                }
            });
        }catch(java.util.concurrent.RejectedExecutionException busy){MessageHelpers.showLongMessage(mContext,"Another settings import is in progress");}
    }

    private void unpackTempZip(Uri zipUri) {
        if (zipUri == null) {
            return;
        }

        File mediaDir = FileHelpers.getExternalMediaDirectory(mContext);

        // Copy ZIP from URI to the temporary file
        String backupZipName = getSavedBackupZipName();
        File tempZip = new File(mContext.getCacheDir(), "incoming-settings.zip");
        copyUriToFile(zipUri, tempZip);

        unpackTempZip(tempZip);
    }

    private void copyUriToDir(Uri uri, File targetDir) {
        try {
            String fileName = getFileName(uri);
            if (fileName == null) fileName = "imported_" + DateHelper.createTimestamp();

            File outFile = new File(targetDir, fileName);

            InputStream in = mContext.getContentResolver().openInputStream(uri);
            OutputStream out = new FileOutputStream(outFile);

            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }

            in.close();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Failed to copyUriToDir", e);
        }
    }

    private void copyUriToFile(Uri uri, File outFile) {
        if(uri==null || !("content".equals(uri.getScheme()) || "file".equals(uri.getScheme())))
            throw new IllegalArgumentException("Only local backup files are accepted");
        try(java.io.InputStream input=mContext.getContentResolver().openInputStream(uri);
            java.io.FileOutputStream output=new java.io.FileOutputStream(outFile)){
            if(input==null)throw new java.io.IOException("No backup stream");
            byte[] buffer=new byte[8192];long total=0;int count;
            while((count=input.read(buffer))!=-1){total+=count;if(total>100L*1024*1024)throw new java.io.IOException("Backup file too large");output.write(buffer,0,count);}
        }catch(Exception failure){outFile.delete();throw new IllegalStateException("Cannot read settings backup",failure);}
    }

    private String getFileName(Uri uri) {
        Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            cursor.moveToFirst();
            String name = cursor.getString(nameIndex);
            cursor.close();
            return name;
        }
        return null;
    }

    private GeneralData getGeneralData() {
        return GeneralData.instance(mContext);
    }

    private String createBackupZipNameWithTimestamp() {
        return mContext.getPackageName() + "_" + getTimeStamp() + ".zip";
    }

    private String getTimeStamp() {
        File timestampFile = getTimestampFile();
        if (timestampFile.exists()) {
            return FileHelpers.getFileContents(timestampFile);
        }

        String timestamp = DateHelper.createTimestamp();
        FileHelpers.stringToFile(timestamp, timestampFile);
        return timestamp;
    }

    private void deleteTimeStamp() {
        File timestampFile = getTimestampFile();
        if (timestampFile.exists()) {
            timestampFile.delete();
        }
    }

    private File getTimestampFile() {
        File mediaDir = FileHelpers.getExternalMediaDirectory(mContext);
        File timestampFile = new File(mediaDir, "timestamp.txt");
        return timestampFile;
    }

    private String getSavedBackupZipName() {
        String oldBackupZipName = getGeneralData().getBackupZipName();
        if (oldBackupZipName == null || !oldBackupZipName.endsWith(".zip")) {
            oldBackupZipName = createBackupZipNameWithTimestamp();
            getGeneralData().setBackupZipName(oldBackupZipName);
        }
        return oldBackupZipName;
    }
}
