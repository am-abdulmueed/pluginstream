package com.google.android.abdul.protube;

import android.os.Bundle;

public class DownloadFromIntentFilter extends MainActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        load(true);
    }
}
