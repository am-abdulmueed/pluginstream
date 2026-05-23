package com.lagradost.cloudstream3.ui.protube.webview;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;

public class YTProWebView extends WebView {
	
	public YTProWebView(Context context) {
		super(context);
	}
	
	public YTProWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public YTProWebView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}
	
	@Override
	protected void onWindowVisibilityChanged(int visibility) {
		
		
		SharedPreferences prefs = getContext().getSharedPreferences("YTPRO", Context.MODE_PRIVATE);
		boolean bgPlay = prefs.getBoolean("bgplay", false); 
		
		
		
		if (visibility != View.GONE && visibility != View.INVISIBLE || !bgPlay){
			super.onWindowVisibilityChanged(visibility);
		}
	}
}
