package com.acbull.activitymonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by apple on 16/1/21.
 */
public class static_receiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!monitor.is_existed(context))
        {
            Intent i = new Intent(context, myservice.class);
            i.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            context.startService(i);
        }
    }
}