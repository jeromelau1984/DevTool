package com.lejia.devtool;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;

public class LauncherApplication extends Application{

	private static Context mContext;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		if (!isMainProcess(this)) {
			try {
				throw new Exception("Service process is running, Ignore.");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		
		mContext = this;
		
	}
	
    private boolean isMainProcess(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo appProcess : mActivityManager.getRunningAppProcesses()) {
            if (appProcess.pid == pid) {
                if (appProcess.processName.equalsIgnoreCase(context.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }
	
    public static Context getContext() {
        if (mContext == null) {
            throw new RuntimeException("Unknown Error");
        }
        return mContext;
    }
	
}
