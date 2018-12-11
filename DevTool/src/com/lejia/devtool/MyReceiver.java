package com.lejia.devtool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import android.os.SystemProperties;

public class MyReceiver extends BroadcastReceiver {
    private static final String TAG = "MyReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Intent cService;

        Log.d(TAG, "onReceive(), action:" + action);
        if (Intent.ACTION_MEDIA_MOUNTED.equals(action) || Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            cService = new Intent(context, DevService.class);
            context.startService(cService);

            boolean bIsVirusScanClose = SystemProperties.get("persist.virus.scan.close", "0").equals("1");
            //start monitor service to kill virus
            if(!bIsVirusScanClose) {
                cService = new Intent(context, VirusKillerService.class);
                context.startService(cService);
            }
        } else if ("android.intent.action.ACTION_DUMP_LOG".equals(action)) {
		    cService = new Intent(context, DumpLogService.class);
		    context.startService(cService);
        }
    }
}
