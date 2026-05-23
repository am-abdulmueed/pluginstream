package com.google.android.abdul.protube.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.abdul.protube.webview.YTProWebView;

public class MediaCommandReceiver extends BroadcastReceiver {
    private final YTProWebView web;

    public MediaCommandReceiver(YTProWebView web) {
        this.web = web;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getExtras() == null) return;
        
        String action = intent.getExtras().getString("actionname");
        Log.e("Action MainActivity", action);

        switch (action) {
            case "PLAY_ACTION":
                web.evaluateJavascript("playVideo();", null);
                break;
            case "PAUSE_ACTION":
                web.evaluateJavascript("pauseVideo();", null);
                break;
            case "NEXT_ACTION":
                web.evaluateJavascript("playNext();", null);
                break;
            case "PREV_ACTION":
                web.evaluateJavascript("playPrev();", null);
                break;
            case "SEEKTO":
                web.evaluateJavascript("seekTo('" + intent.getExtras().getString("pos") + "');", null);
                break;
        }
    }
}
