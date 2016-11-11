package com.acbull.activitymonitor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by apple on 16/3/9.
 */
public class SubActivity extends Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent i = getIntent();
        Log.i("new", "from sub:" + i.toURI());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
