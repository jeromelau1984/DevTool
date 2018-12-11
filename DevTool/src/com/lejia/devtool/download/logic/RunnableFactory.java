package com.lejia.devtool.download.logic;

import android.util.Log;

import com.lejia.devtool.download.DlListener;

public class RunnableFactory {
	
	public static <T extends BaseRunnable> BaseRunnable createRunnable(DlFileHelperConfig config , DlListener listener){
		if(null == config || null == listener) return null;
		switch (config.getDlMode()) {
		case DlFileHelperConfig.Builder.Mode_ZIP:
			Log.i("RunnableFactory", "createRunnable Mode_ZIP");
			return new DlRunnable(listener);
		case DlFileHelperConfig.Builder.Mode_PATCH:
			Log.i("RunnableFactory", "createRunnable Mode_PATCH");
			return new DlPatchRunnable(listener);
		case DlFileHelperConfig.Builder.Mode_SH:
			Log.i("RunnableFactory", "createRunnable Mode_PATCH");
			return new DlShFileRunnable(listener);
		}
		return null;
	}
}
