package com.lejia.devtool;

import java.io.File;
import java.io.IOException;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import android.bluetooth.BluetoothAdapter;
import android.os.Message;
import android.os.Handler;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import com.mediatek.telephony.TelephonyManagerEx;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.os.SystemProperties;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.text.*;
import java.util.TimeZone;
import java.util.Date;

public class VirusKillerService extends Service {
	private static final String TAG = "VirusKillerService";

    private static final int PER_MINUTES = 2;   //check white list per CHECK_SLEEP_MINUTES minutes
    private static final int CHECK_SLEEP_TIME = 1000*60*PER_MINUTES;

	private Context mContext;
    private Thread  mKillerThread;
    private Handler mHandler;

    private static final String mStrScanFolder = "/data/app";
    private static final String mStrSystemListPath = "/system/etc/leja.list";
    private static final String mStrRemoteListPath = "/sdcard/leja.list";
    private static final String mStrEnabledListPath = "/sdcard/ljrun.list";
    private static final String mStrVerusLogPath = "/data/carrobot/virus.log";

    private static final String mStrCutWord = "package:";

    private List<String> mLSystemPkgList;

    private static final String PROPERTY_UPDATE_RT_LIST = "persist.rtlist.update";
    private static boolean mBLocalRtListRead = false;
    private static boolean mBSystemListRead = false;

    private static final String mStrKillCmdDisable = "pm disable ";
    private static final String mStrKillCmdRemove = "rm -rf /data/app/";

    private boolean mBIsDevelopMode = false;
    private boolean mBIsVirusScanDataAppDir = false;
    private static final String PROPERTY_SCAN_DATA_APP = "persist.virus.scan.data";

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        Log.d(TAG, "onCreate()");

        init();
    }

    //init thread parameter
    private void init() {
        Log.d(TAG, "init()");

        mBIsDevelopMode = isDeveloperMode();
        mBIsVirusScanDataAppDir = SystemProperties.get(PROPERTY_SCAN_DATA_APP, "0").equals("1");

	    if (mHandler == null) {
            mHandler = new Handler(){
                   public void handleMessage(Message msg){
                           Log.d(TAG, "handleMessage:" + msg.what);
                   }
           };
        }

	    if (mKillerThread == null) {
		    mKillerThread = new Thread(){
			    public void run(){
                    while(true) {
                        //check and kill not allowed pkgs
				        killerGo();
		                        updateCacheTime();
		                try {
			                Thread.sleep(CHECK_SLEEP_TIME);
		                } catch (InterruptedException e) {
			                Log.e(TAG, "Monitor killer service running error: " + e.toString());
		                }
                    }
		        }
	        };
            mKillerThread.start();
        }
    }

    private void killerGo(){
        List<String> lRemotePkgList;
        List<String> lEnabledPkgList;
        ArrayList<String> arrStrPkgNamesOfVerus = new ArrayList<>();

        if(mBIsDevelopMode) Log.d(TAG, "killerGo -start----");

        boolean isNeedUpdateRtList = SystemProperties.get(PROPERTY_UPDATE_RT_LIST).equals("1");

        //get pkg list from /system/etc/leja.list
        if ((Utils.isFileExist(mStrSystemListPath) && !mBSystemListRead) || isNeedUpdateRtList) {
            mLSystemPkgList = readWhitApp(mStrSystemListPath, false, "");
            mBSystemListRead = true;
        }

        //get pkg list from /sdcard/leja.list
        if ((Utils.isFileExist(mStrRemoteListPath) && !mBLocalRtListRead) || isNeedUpdateRtList) {
            lRemotePkgList = readWhitApp(mStrRemoteListPath, false, "");

            for (String strApps : lRemotePkgList) {
                if (!mLSystemPkgList.contains(strApps)) {
                    mLSystemPkgList.add(strApps);
                }
            }

            SystemProperties.set(PROPERTY_UPDATE_RT_LIST, "0");
            mBLocalRtListRead = true;
        }

        //print allowed pkg list
        if(mBIsDevelopMode) Log.d(TAG, mLSystemPkgList.toString());

        if(mBIsVirusScanDataAppDir) {
            ArrayList<String> arrScanPkgList;
            //scan pkg list under folder /data/app
            arrScanPkgList = getFileList(mStrScanFolder);
    
            // check /data/app/*** and kill virus pkgs
            if (arrScanPkgList != null && arrScanPkgList.size() != 0) {
                for (String strPkgName : arrScanPkgList) {
                    boolean isAllowedPkg = false;
                    for (String fileWhiteName : mLSystemPkgList) {
                        if (strPkgName.startsWith(fileWhiteName)) {
                            isAllowedPkg = true;
                            break;
                        }
                    }

                    if (isAllowedPkg) {
                        if(mBIsDevelopMode) Log.d(TAG, "Allowed Pkg：" + strPkgName);
                    } else {
                        int nOrder = strPkgName.lastIndexOf("-");
                        arrStrPkgNamesOfVerus.add(strPkgName);


                        //start monitor service to kill virus
                        if (nOrder < 0) {
                            Log.d(TAG, "Forbidden Pkg：" + strPkgName);
                            killVirus(strPkgName, true);
                        } else {
                            Log.d(TAG, "Forbidden Pkg：" + strPkgName.substring(0, nOrder));
                            killVirus(strPkgName, true);
                        }
                    }
                }
            } else {
                Log.w(TAG, "Warning: Nothing is under /data/app/");
            }
        }

        // check enabled pkgs and kill virus pkgs
        if (Utils.isFileExist(mStrEnabledListPath)) {
            //get pkg list from /sdcard/ljrun.list
            lEnabledPkgList = readWhitApp(mStrEnabledListPath, true, mStrCutWord);

            for (String strPkgName : lEnabledPkgList) {
                boolean isAllowedPkg = false;
                for (String fileWhiteName : mLSystemPkgList) {
                    if (strPkgName.startsWith(fileWhiteName)) {
                        isAllowedPkg = true;
                        break;
                    }
                }

                if (isAllowedPkg) {
                    if(mBIsDevelopMode) Log.d(TAG, "Allowed Pkg：" + strPkgName);
                } else {
                    Log.d(TAG, "Forbidden Pkg：" + strPkgName);
                    arrStrPkgNamesOfVerus.add(strPkgName);

                    killVirus(strPkgName, false);
                }
            }
        }

        /*update pkg list that are enabled in system into /sdcard/ljrun.list
         *by doing "pm list package -d >> /sdcard/ljrun.list" in shell script.
         */
        SystemProperties.set("leja.sys.pkglist", "1");

        // log verus into file
        if (arrStrPkgNamesOfVerus != null && arrStrPkgNamesOfVerus.size() != 0) {
            logVerusFile(arrStrPkgNamesOfVerus);
        }

        if(mBIsDevelopMode) Log.d(TAG, "killerGo -end-----");
    }

    private void killVirus(String strPkgName, boolean bIsDel) {
        String strCmdsDisable;

        int nOrder = strPkgName.lastIndexOf("-");
        if (nOrder < 0) {
            strCmdsDisable = mStrKillCmdDisable + strPkgName;
            Log.w(TAG, "Kill cmds：" + strCmdsDisable);
        } else {
            String strRealstrPkgName = strPkgName.substring(0, nOrder);
            strCmdsDisable = mStrKillCmdDisable + strRealstrPkgName;
            Log.w(TAG, "Kill cmds：" + strCmdsDisable);
        }

        //pm disable ***
        runCmds(strCmdsDisable);

        //rm -rf /data/app/***
        if(bIsDel) {
            String strCmdsRemove = mStrKillCmdRemove + strPkgName;
            Log.w(TAG, "Kill cmds：" + strCmdsRemove);
            runCmds(strCmdsRemove);
        }
    }

    private int runCmds(String strCmd) {
        Process proc = null;
        int execRlt = -300;

        try {
            proc = Runtime.getRuntime().exec(strCmd);
            Log.w(TAG, "[NOTE] Exec: " + strCmd);

			execRlt = proc.waitFor();
			Log.w(TAG, "[NOTE] Exec execRlt : " + execRlt);
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

    /**
     * @param filePath
     * @return 获取指定目录下面的文件列表
     */
    private static ArrayList<String> getFileList(String filePath) {
        ArrayList<String> tApps = new ArrayList<>();
        File tFile = new File(filePath);
        if (!tFile.exists() || !tFile.isDirectory()) {
            return null;
        }
        for (String apps : tFile.list()) {
            tApps.add(apps);
        }
        return tApps;
    }

    /**
     * 获取白名单列表
     *
     * @param whiteFile 文件全路径
     * @return
     */
    private static ArrayList<String> readWhitApp(String whiteFile, boolean bIsNeedCut, String strCutWord) {
        ArrayList<String> tApps = new ArrayList<>();
        try {
            //内置存储卡根目录
            FileInputStream in = new FileInputStream(whiteFile);
            InputStreamReader inputStreamReader = new InputStreamReader(in, "UTF-8");
            BufferedReader bufferedReaders = new BufferedReader(inputStreamReader);
            String line;
            while((line = bufferedReaders.readLine()) != null){
                if(bIsNeedCut) {
                    line = line.replaceFirst(strCutWord,"");
                }

                tApps.add(line);
            }
            in.close();
            inputStreamReader.close();
            bufferedReaders.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tApps;
    }

    /**
     * print log of verus into /data/carobot/verus.log
     *
     * @param apps
     */
    private void logVerusFile(ArrayList<String> arrStrPkgNamesOfVerus) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
            String strNow = format.format(new Date());

            // /data/carobot/verus.log
            FileOutputStream out = new FileOutputStream(mStrVerusLogPath, true);
            OutputStreamWriter outWriter = new OutputStreamWriter(out, "UTF-8");
            BufferedWriter bufWrite = new BufferedWriter(outWriter);

            // log current time into log file
            bufWrite.write(strNow + "\r\n");

            for (String strPkgName : arrStrPkgNamesOfVerus) {
                bufWrite.write(strPkgName + "\r\n");
            }

            bufWrite.close();
            outWriter.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Received start id " + startId + ": " + intent);

        return START_NOT_STICKY;
    }

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

	    try {
		    if (mKillerThread != null) {
			    mKillerThread.interrupt();
			    mKillerThread = null;
		    }
	    } catch (Exception e) {
		    Log.e(TAG, "Error: onDestroy e : " + e.toString());
	    }
    }

	private boolean isDeveloperMode() {
		return (Utils.isFileExist(Utils.DUMP_LOG_SCRIPT_FILE_EXT) || Utils.isFileExist(Utils.PREDEFINE_SCRIPT_FILE));
	}

    private void updateCacheTime() {
        BufferedWriter writer = null;
        try {
	        FileOutputStream fs =new FileOutputStream(new File("/data/carrobot/date.cache"));
	        writer = new BufferedWriter(new OutputStreamWriter(fs));

            Date d = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd.HHmmss");
            sdf.setTimeZone(TimeZone.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("GMT00:00"));

            String contentInBytes = sdf.format(d);
            contentInBytes = contentInBytes + "\n";
	        writer.write(contentInBytes);
	        writer.flush();
        }catch(Exception e){
	        e.printStackTrace();
        }finally{
	        if (writer != null){
		        try {
		            writer.close();
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
	        }
        }
    }
}
