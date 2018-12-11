package com.lejia.devtool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import android.media.MediaPlayer;

public class DumpLogService extends IntentService {

	private static final String TAG = "DumpLogService";

	private static final String ACTION_DUMPLOG_FINISH = "com.ileja.carrobot.action.ACTION_DUMP_LOG_FINISH";
	
	public static final String ONLINE_CONFIG_FILE = "/sdcard/config.dat";
	public static final String KEY_DUMPLOG = "dumplog";// 保存文件的dumplog key, 用于DevTool里面控制抓取文件开关.

	private Context mContext;
	private File mDumpLogScriptFile = null;
	public DumpLogService() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;
		mDumpLogScriptFile = new File(getScriptPath());
		try {
			copyScriptFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Log.i(TAG, "onCreate()");
	}

	/**
     * 复制脚本文件到目的目录 ，只做不存在文件时的第一次拷贝，之后的脚本更新通过app更新实现
     * 
     * @param context
     */
    private synchronized void copyScriptFile() throws IOException {
        if (!mDumpLogScriptFile.exists()) {
        InputStream is = null;
        long startTimestemp = System.currentTimeMillis();
			Log.d(TAG, "ScriptFile: COPY START ");
	        is = mContext.getAssets().open(Utils.DUMP_LOG_SCRIPT_FILE_NAME);
			OutputStream os = new FileOutputStream(mDumpLogScriptFile);
			byte[] buffer = new byte[100 * 1024];
			int length;
			while ((length = is.read(buffer)) > 0) {
				os.write(buffer, 0, length);
				os.flush();
			}
			if (is != null) {
				is.close();
				is = null;
			}
			if (os != null) {
				os.close();
				os = null;
			}
	        Log.d(TAG, "ScriptFile: COPY END ,cost time:"+(System.currentTimeMillis() - startTimestemp));
		}
    }
    
    private void soundTip() {
	if(isDeveloperMode()) {
	    MediaPlayer player = null;
	    player = MediaPlayer.create(mContext, R.raw.log_dump_tip);
	    player.start();
	}
    }
	
	private void runScript() {
		if (mDumpLogScriptFile.exists() && isLogSwitchOn()) {
			// DeveloperMode: play sound when to catch log
			soundTip();

			mDumpLogScriptFile.setExecutable(true, false);
			Process proc = null;
			
			try {
				proc = Runtime.getRuntime().exec("sh " + getScriptPath());
				Log.i(TAG, "proc = " + proc);
				
				if (isDeveloperMode()) {
					Toast.makeText(mContext, "[NOTE] Exec: " + getScriptPath(), Toast.LENGTH_LONG).show();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (proc != null) {
						int ret = proc.waitFor();
						
						Log.i(TAG, "waitFor ret = " + ret);
						
						if (ret == 0) {
							Log.i(TAG, "exit value = " + proc.exitValue());
							// 非开发模式: 发送广播出去告知log已经抓取完成并上传服务器
							/*if(!isDeveloperMode())*/
								mContext.sendBroadcast(new Intent(ACTION_DUMPLOG_FINISH));
						}
						
						proc.destroy();
					} else {
						Log.e(TAG, "proc is null");
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.i(TAG, "onHandleIntent: " + intent);
		runScript();
	}

	public static Properties loadConfig(String file) {
		Properties properties = new Properties();
		try {
			FileInputStream s = new FileInputStream(file);
			properties.load(s);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return properties;
	}

	private boolean isLogSwitchOn() {
		String dumpLog = loadConfig(ONLINE_CONFIG_FILE).getProperty(KEY_DUMPLOG);
		boolean dumpLogEnable = false;
		try {
			if (dumpLog != null) {
				dumpLogEnable = (Integer.parseInt(dumpLog) == 1);
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (isDeveloperMode() || dumpLogEnable) {
				Log.i(TAG, "dumpLogEnable = true");
				return true;
			}
		}

		Log.i(TAG, "dumpLogEnable = false");
		//return false;
		return true;//Always switch on
	}

	private String getScriptPath() {
		boolean isExtScriptExist = Utils.isFileExist(Utils.DUMP_LOG_SCRIPT_FILE_EXT);
		Log.i(TAG, "[Jerome_append] DUMP_LOG getScriptPath : " + (isExtScriptExist ? Utils.DUMP_LOG_SCRIPT_FILE_EXT : Utils.DUMP_LOG_SCRIPT_FILE));
		//return isExtScriptExist ? Utils.DUMP_LOG_SCRIPT_FILE_EXT : Utils.DUMP_LOG_SCRIPT_FILE;
		return Utils.DUMP_LOG_SCRIPT_FILE;
	}

	private boolean isDeveloperMode() {
		return (Utils.isFileExist(Utils.DUMP_LOG_SCRIPT_FILE_EXT) || Utils.isFileExist(Utils.PREDEFINE_SCRIPT_FILE));
	}
}
