package com.lejia.devtool.upload;

import java.io.File;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import com.lejia.devtool.Utils;
import java.util.StringTokenizer;
import com.lejia.devtool.download.logic.DlFileHelper;
import com.lejia.devtool.download.logic.DlFileHelperConfig;
import com.lejia.devtool.FileUtil;
import com.lejia.devtool.SMSReceiver;
import com.lejia.devtool.LauncherApplication;

public class UploadDumpLogService extends IntentService {

	
	private static final String TAG = "UploadDumpLogService";

	public static final String GLOBAL_SERVER_DOMAIN = "http://wx.ileja.com/service/"; //Test service:http://test.ileja.cn/service/

	public static final String DUMP_LOG_DEST_FILE = "/storage/sdcard0/ileja_syslogs/";
    
	public UploadDumpLogService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.i(TAG, "onHandleIntent intent:" + intent);
		String uploadStr = intent.getStringExtra(SMSReceiver.UPLOAD_SYSLOG_EXTRA_KEY);
		
		if(TextUtils.isEmpty(uploadStr)){
			return;
		}
		boolean isUploadExecRlt = intent.getBooleanExtra(SMSReceiver.UPLOAD_EXECCMD_EXTRA_KEY, false);
		Log.i(TAG,"isUploadExecRlt : " + isUploadExecRlt);
		if(isUploadExecRlt){
			File execCmdLog = new File(uploadStr);
			if(execCmdLog.exists()){
				uploadLog(execCmdLog);
			}			
		}else{
			int rlt = runScript(uploadStr);
        	Log.i(TAG, "Jerome rlt : " + rlt);
            
        	if(rlt >= 0){
        		File dumpLogDestFileDir = new File(DUMP_LOG_DEST_FILE);
				File[] dumpLogDestFiles = dumpLogDestFileDir.listFiles();
				String endPref = ".tar.gz";
				if (dumpLogDestFiles != null) {
					for (File logFile : dumpLogDestFiles) {
						Log.d(TAG, "logFile: " + logFile);
						if (logFile != null && logFile.exists() && logFile.getName().endsWith(endPref)) {
							boolean result = uploadLog(logFile);
							if (result) {
								boolean deleted = logFile.delete();
								Log.i(TAG, "uploadLog success, delete: " + logFile + " ,ret:" + deleted);
							} else {
								Log.w(TAG, "uploadLog " + logFile + " failed!");
							}
						}
					}
				} else {
					Log.w(TAG, "dumpLogDestZipFiles is null");
				}
        	}
		}
	}
	
	private static boolean uploadLog(final File file) {
		final String url = GLOBAL_SERVER_DOMAIN + "device/upload/log?"+ ComNetParam.getCommonParam() +"&uptype=log";
		if(!TextUtils.isEmpty(url)){
			Log.i(TAG, "uploadLog url : " + url);
		}
		String result = ServerHttpUtils.postFile(url, file.getAbsolutePath());
		Log.i(TAG, "uploadLog result:" + result);

		if (!TextUtils.isEmpty(result)) {
			JSONObject json = null;
			try {
				json = new JSONObject(result);
				if ("2000".equals(json.optString("status"))) {
					return true;
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		return false;
	}

	private synchronized int runScript(String filePath) {
		if(TextUtils.isEmpty(filePath)){
			return -100;
		}
		if (Utils.isFileExist(filePath)) {
			File file = new File(filePath);
			file.setExecutable(true, false);
			Log.i(TAG, "runScript filePath: " + filePath);
			Log.i(TAG, "runScript file.canExecute : " + file.canExecute());
		}
			Process proc = null;
			int execRlt = -300;

			try {
				proc = Runtime.getRuntime().exec(filePath);				
				Log.i(TAG, "[NOTE] Exec: " + filePath);
				execRlt = proc.waitFor();
				Log.i(TAG, "[NOTE] Exec execRlt : " + execRlt);
				proc.destroy();
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG, "IOException : " + e.toString());
				execRlt = -400;
			} catch (InterruptedException e) {
				e.printStackTrace();
				execRlt = -500;
			} finally {

				
			}

			return execRlt;
        
    }
}
