package com.google.android.abdul.protube;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.net.*;
import android.util.*;
import android.webkit.*;
import android.content.pm.*;
import android.media.AudioManager;
import java.util.*;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import com.google.android.abdul.protube.webview.BinaryStreamManager;
import com.google.android.abdul.protube.webview.YTProWebView;
import com.google.android.abdul.protube.webview.YTProWebViewClient;
import com.google.android.abdul.protube.webview.YTProWebChromeClient;
import com.google.android.abdul.protube.webview.WebAppInterface;
import com.google.android.abdul.protube.receivers.MediaCommandReceiver;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    public boolean portrait = false;
    private MediaCommandReceiver mediaReceiver;
    private AudioManager audioManager;

    public boolean isPlaying = false;
    public boolean mediaSession = false;
    public boolean isPip = false;

    private YTProWebView web;
    public BinaryStreamManager streamManager;
    private OnBackInvokedCallback backCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        SharedPreferences prefs = getSharedPreferences("YTPRO", MODE_PRIVATE);
        int launches = prefs.getInt("launch_count", 0) + 1;
        prefs.edit().putInt("launch_count", launches).apply();

        if (!prefs.contains("bgplay")) {
            prefs.edit().putBoolean("bgplay", true).apply();
        }

        load(false);

        MainActivity.this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void load(boolean dl) {

        web = findViewById(R.id.web);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        streamManager = new BinaryStreamManager(web, this);

        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setSupportZoom(true);
        web.getSettings().setBuiltInZoomControls(true);
        web.getSettings().setDisplayZoomControls(false);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setDatabaseEnabled(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false); // Allow autoplay
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        web.addJavascriptInterface(new WebAppInterface(this, web), "Android");
        web.setWebViewClient(new YTProWebViewClient(this, web));
        web.setWebChromeClient(new YTProWebChromeClient(this, web));

        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();
        String url = "https://m.youtube.com/";
        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            url = data.toString();
        } else if (Intent.ACTION_SEND.equals(action)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null && (sharedText.contains("youtube.com") || sharedText.contains("youtu.be"))) {
                url = sharedText;
            }
        }
        web.loadUrl(url);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(web, true); // 3rd party cookies 🍪
        }

        setReceiver();

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            OnBackInvokedDispatcher dispatcher = getOnBackInvokedDispatcher();
            backCallback = new OnBackInvokedCallback() {
                @Override
                public void onBackInvoked() {
                    if (web.canGoBack()) {
                        web.goBack();
                    } else {
                        finish();
                    }
                }
            };
            dispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCallback);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                web.loadUrl("https://m.youtube.com");
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.grant_mic), Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(getApplicationContext(), getString(R.string.grant_storage), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) {
            web.goBack();
        } else {
            finish();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        web.loadUrl(isInPictureInPictureMode ? "javascript:PIPlayer();" : "javascript:removePIP();", null);
        isPip = isInPictureInPictureMode;
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (android.os.Build.VERSION.SDK_INT >= 26 && web.getUrl().contains("watch")) {
            if (isPlaying) {
                try {
                    PictureInPictureParams params;
                    isPip = true;
                    if (portrait) {
                        params = new PictureInPictureParams.Builder().setAspectRatio(new Rational(9, 16)).build();
                    } else {
                        params = new PictureInPictureParams.Builder().setAspectRatio(new Rational(16, 9)).build();
                    }
                    enterPictureInPictureMode(params);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void setReceiver() {
        mediaReceiver = new MediaCommandReceiver(web);
        IntentFilter filter = new IntentFilter("TRACKS_TRACKS");
        if (Build.VERSION.SDK_INT >= 34 && getApplicationInfo().targetSdkVersion >= 34) {
            registerReceiver(mediaReceiver, filter, RECEIVER_EXPORTED);
        } else {
            registerReceiver(mediaReceiver, filter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService(new Intent(getApplicationContext(), ForegroundService.class));
        if (mediaReceiver != null) unregisterReceiver(mediaReceiver);
        if (streamManager != null) streamManager.cleanup();
        if (android.os.Build.VERSION.SDK_INT >= 33 && backCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
        }
    }
}
