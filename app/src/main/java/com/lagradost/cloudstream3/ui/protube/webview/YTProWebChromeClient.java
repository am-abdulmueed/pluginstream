package com.lagradost.cloudstream3.ui.protube.webview;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;

import com.lagradost.cloudstream3.ui.protube.ProTubeFragment;

public class YTProWebChromeClient extends WebChromeClient {
    private final ProTubeFragment fragment;
    private final YTProWebView web;
    
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private int mOriginalOrientation;
    private int mOriginalSystemUiVisibility;

    public YTProWebChromeClient(ProTubeFragment fragment, YTProWebView web) {
        this.fragment = fragment;
        this.web = web;
    }

    @Override
    public Bitmap getDefaultVideoPoster() {
       return BitmapFactory.decodeResource(fragment.requireContext().getApplicationContext().getResources(), 2130837573);
    }

    @Override
    public void onShowCustomView(View paramView, WebChromeClient.CustomViewCallback viewCallback) {
        // 1. Determine orientation for FULL SCREEN
        mOriginalOrientation = fragment.portrait ?
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT :
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;

        if (fragment.isPip) mOriginalOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            fragment.requireActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            WindowManager.LayoutParams params = fragment.requireActivity().getWindow().getAttributes();
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            fragment.requireActivity().getWindow().setAttributes(params);
        }

        if (mCustomView != null) {
            onHideCustomView();
            return;
        }

        mCustomView = paramView;
        mOriginalSystemUiVisibility = fragment.requireActivity().getWindow().getDecorView().getSystemUiVisibility();
        
        // 2. Set the activity to full screen orientation (Landscape usually)
        fragment.requireActivity().setRequestedOrientation(mOriginalOrientation);
        
        // Store portrait so onHideCustomView knows what to go back to
        mOriginalOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

        mCustomViewCallback = viewCallback;
        ((FrameLayout) fragment.requireActivity().getWindow().getDecorView()).addView(mCustomView, new FrameLayout.LayoutParams(-1, -1));
        fragment.requireActivity().getWindow().getDecorView().setSystemUiVisibility(3846);
    }

    @Override
    public void onHideCustomView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            fragment.requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            WindowManager.LayoutParams params = fragment.requireActivity().getWindow().getAttributes();
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
            fragment.requireActivity().getWindow().setAttributes(params);
        }

        ((FrameLayout) fragment.requireActivity().getWindow().getDecorView()).removeView(mCustomView);
        mCustomView = null;
        fragment.requireActivity().getWindow().getDecorView().setSystemUiVisibility(mOriginalSystemUiVisibility);
        
        // 3. Set the activity BACK to the orientation saved right after going full screen (Portrait)
        fragment.requireActivity().setRequestedOrientation(mOriginalOrientation);
        
        // Reset state for the next time we enter full screen
        mOriginalOrientation = fragment.portrait ?
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT :
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;

        mCustomViewCallback = null;
        web.clearFocus();
    }

    @Override
    public void onPermissionRequest(final PermissionRequest request) {
        if (Build.VERSION.SDK_INT > 22 && request.getOrigin().toString().contains("youtube.com")) {
            if (fragment.requireActivity().checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
                fragment.requireActivity().requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 101);
            } else {
                request.grant(request.getResources());
            }
        }
    }
}
