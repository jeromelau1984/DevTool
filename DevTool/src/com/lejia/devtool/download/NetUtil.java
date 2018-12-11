package com.lejia.devtool.download;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.lejia.devtool.LauncherApplication;

public class NetUtil {

	private static final String TAG = "NetUtil";
	
	private static NetUtil mInstance;
	
	private NetUtil(){}
	
	public static NetUtil getIns() {
		NetUtil result = mInstance;
		if(result == null){
			synchronized (NetUtil.class) {
				result = mInstance;
				if(result == null){
					result = mInstance = new NetUtil();
				}
			}
		}
		return result;
	}
	
	public void release(){
		if(mInstance != null){
			synchronized (mInstance) {
				mInstance = null;
			}
		}
	}

	public static boolean isNetworkAvailable() {
		ConnectivityManager connectivity = (ConnectivityManager) LauncherApplication.getContext()
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity == null) {
			Log.i(TAG, "Network Unavailabel");
			return false;
		} else {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null) {
				for (int i = 0; i < info.length; i++) {
					if (info[i].getState() == NetworkInfo.State.CONNECTED) {
						Log.i(TAG, "Network Available");
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public static boolean isWifiConnected(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Activity.CONNECTIVITY_SERVICE);
		if (connectivityManager == null) {
			return false;
		}
		NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
		if (activeNetInfo != null) {
			if (activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
				Log.i(TAG, "TYPE_WIFI CONNECTED");
				return true;
			}
		}
		return false;
	}

	public static boolean isMobileConnected(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Activity.CONNECTIVITY_SERVICE);
		if (connectivityManager == null) {
			return false;
		}
		NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
		if (activeNetInfo != null) {
			if (activeNetInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
				Log.i(TAG, "TYPE_MOBILE CONNECTED");
				return true;
			}
		}
		return false;
	}

}
