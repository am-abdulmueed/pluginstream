package com.lagradost.cloudstream3.ui.protube.utils;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import com.lagradost.cloudstream3.R;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class DownloadUtils {

    public static void downloadFile(Activity activity, String filename, String url, String mtype) {
        if (Build.VERSION.SDK_INT > 22 && Build.VERSION.SDK_INT < Build.VERSION_CODES.R && 
            activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            
            activity.runOnUiThread(() -> Toast.makeText(activity, R.string.grant_storage, Toast.LENGTH_SHORT).show());
            activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            return;
        }
        
        try {
            String encodedFileName = URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");
            DownloadManager downloadManager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            
            request.setTitle(filename)
                   .setDescription(filename)
                   .setMimeType(mtype)
                   .setAllowedOverMetered(true)
                   .setAllowedOverRoaming(true)
                   .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, encodedFileName)
                   .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE | DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                   
            downloadManager.enqueue(request);
            Toast.makeText(activity, activity.getString(R.string.dl_started), Toast.LENGTH_SHORT).show();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (Exception ignored) {
            Toast.makeText(activity, ignored.toString(), Toast.LENGTH_SHORT).show();
        }
    }
}
