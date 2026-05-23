package com.lagradost.cloudstream3.ui.protube.webview;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebMessagePortCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.widget.Toast;


public class BinaryStreamManager {

    private final YTProWebView webView;
    private final Context context;
    private final ExecutorService ioExecutor = Executors.newFixedThreadPool(4);

    // API 29+ path: store OutputStreams and their URIs
    private final ConcurrentHashMap<String, OutputStream> fileStreams = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Uri> fileUris = new ConcurrentHashMap<>();

    // API 21-28 path: keep FileOutputStream separately
    private final ConcurrentHashMap<String, FileOutputStream> legacyStreams = new ConcurrentHashMap<>();

    public BinaryStreamManager(YTProWebView webView, Context context) {
        this.webView = webView;
        this.context = context;
    }

    public void openStreamForFile(String fileName) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_ARRAY_BUFFER)) {
            Log.e("YTPRO_STREAM", "ArrayBuffer not supported on this device.");
            Toast.makeText(context, "ArrayBuffer not supported on this device.", Toast.LENGTH_SHORT).show();
   
            return;
        }
        

        WebMessagePortCompat[] channel = WebViewCompat.createWebMessageChannel(webView);
        WebMessagePortCompat localPort = channel[0];
        WebMessagePortCompat jsPort = channel[1];

        // 1. Open file stream asynchronously
        ioExecutor.execute(() -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // API 29+ — MediaStore, zero permissions needed
                    ContentResolver resolver = context.getContentResolver();
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Downloads.MIME_TYPE, getMimeType(fileName));
                    values.put(MediaStore.Downloads.RELATIVE_PATH, "Download/YTPRO");
                    values.put(MediaStore.Downloads.IS_PENDING, 1);

                    Uri uri = resolver.insert(
                            MediaStore.Downloads.getContentUri("external"), values);

                    if (uri == null) {
                        Log.e("YTPRO_STREAM", "MediaStore insert returned null for: " + fileName);
                        return;
                    }

                    OutputStream os = resolver.openOutputStream(uri);
                    if (os == null) {
                        Log.e("YTPRO_STREAM", "openOutputStream returned null for: " + fileName);
                        return;
                    }

                    fileStreams.put(fileName, os);
                    fileUris.put(fileName, uri);

                } else {
                    // API 21-28 — direct file access
                    File dir = new File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "YTPRO"
                    );
                    if (!dir.exists()) dir.mkdirs();
                    File file = new File(dir, fileName);
                    FileOutputStream fos = new FileOutputStream(file, true);
                    legacyStreams.put(fileName, fos);
                }

                Log.d("YTPRO_STREAM", "Stream opened for: " + fileName);

            } catch (Exception e) {
                Log.e("YTPRO_STREAM", "Failed to open stream: " + e.getMessage());
            }
        });

        // 2. This port only listens for chunks belonging to THIS file
        localPort.setWebMessageCallback(new WebMessagePortCompat.WebMessageCallbackCompat() {
            @Override
            public void onMessage(WebMessagePortCompat port, WebMessageCompat message) {
                ioExecutor.execute(() -> {
                    if (message.getType() == WebMessageCompat.TYPE_ARRAY_BUFFER) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                OutputStream os = fileStreams.get(fileName);
                                if (os != null) os.write(message.getArrayBuffer());
                            } else {
                                FileOutputStream fos = legacyStreams.get(fileName);
                                if (fos != null) fos.write(message.getArrayBuffer());
                            }
                        } catch (Exception e) {
                            Log.e("YTPRO_STREAM", "Write failed: " + e.getMessage());
                        }

                    } else if (message.getType() == WebMessageCompat.TYPE_STRING
                            && "END".equals(message.getData())) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                OutputStream os = fileStreams.remove(fileName);
                                if (os != null) {
                                    os.flush();
                                    os.close();
                                }
                                // Mark file as visible
                                Uri uri = fileUris.remove(fileName);
                                if (uri != null) {
                                    ContentValues values = new ContentValues();
                                    values.put(MediaStore.Downloads.IS_PENDING, 0);
                                    context.getContentResolver().update(uri, values, null, null);
                                }
                            } else {
                                FileOutputStream fos = legacyStreams.remove(fileName);
                                if (fos != null) {
                                    fos.flush();
                                    fos.close();
                                }
                            }

                            port.close();
                            Log.d("YTPRO_STREAM", "Stream finished for: " + fileName);

                        } catch (Exception e) {
                            Log.e("YTPRO_STREAM", "Close failed: " + e.getMessage());
                        }
                    }
                });
            }
        });

        // 3. Send the port back to JS tagged with the filename
        WebViewCompat.postWebMessage(
                webView,
                new WebMessageCompat("PORT_FOR:" + fileName, new WebMessagePortCompat[]{jsPort}),
                Uri.EMPTY
        );
    }

    // Returns the Uri for a file already written by this app (for muxer input)
    public Uri getUriForFile(String fileName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return fileUris.get(fileName);
        }
        return null;
    }

    private String getMimeType(String fileName) {
        if (fileName.endsWith(".webm")) return "video/webm";
        if (fileName.endsWith(".mp4"))  return "video/mp4";
        if (fileName.endsWith(".m4a"))  return "audio/mp4";
        if (fileName.endsWith(".opus")) return "audio/ogg";
        return "application/octet-stream";
    }

    public void cleanup() {
        ioExecutor.shutdown();
    }
}
