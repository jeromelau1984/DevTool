package com.lejia.devtool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;

import android.os.SystemProperties;
import android.util.Log;

import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.content.Context;
import android.provider.Settings;

import com.mediatek.telephony.TelephonyManagerEx;

public class Utils {
    private static final String TAG = "Utils";

    public static final String PREDEFINE_SCRIPT_FILE = "/storage/sdcard1/carrobot.sh";
    
    public static final String DUMP_LOG_SCRIPT_FILE_NAME = "carrobot_dumplog.sh";
    public static final String DUMP_LOG_SCRIPT_FILE = "/storage/sdcard0/" + DUMP_LOG_SCRIPT_FILE_NAME;
    public static final String DUMP_LOG_SCRIPT_FILE_EXT = "/storage/sdcard1/carrobot_dumplog.sh";

    public static final boolean MTK_C2K_SUPPORT = SystemProperties.get("ro.mtk_c2k_support")
            .equals("1");

    public static String readTxtFile(String filePath){
    	String lineTxt = null;
        try {
            File file = new File(filePath);
            if(file.isFile() && file.exists()) {
            	InputStreamReader read = new InputStreamReader(new FileInputStream(file));
            	BufferedReader bufferedReader = new BufferedReader(read);
            	lineTxt = bufferedReader.readLine();
            	read.close();
	        } else {
	            throw new RuntimeException("Invalid file path:" + filePath);
	        }
        } catch (Exception e) {
            Log.e(TAG, "Read file(" + filePath + ") meets exception");
            e.printStackTrace();
        }
        
        if (lineTxt != null) {
        	lineTxt.trim();
        }
        
        return lineTxt;
    }
    
    public static boolean isFileExist(String filePath) {
    	boolean bExist = false;
    	
    	File file = new File(filePath);
    	if(file.isFile() && file.exists()) {
    		bExist = true;
    	} else {
    		bExist = false;
    	}
    	
    	Log.d(TAG, "isFileExist(" + filePath + ") " + bExist);
    	return bExist;
    }
    
    public static void writeTxtFile(String filePath, String context){
        try {
            File file = new File(filePath);
            if(file.isFile() && file.exists()) {
            	FileWriter fileWriter = new FileWriter(file);
            	fileWriter.write(context);
            	fileWriter.flush();
            	fileWriter.close();
	        } else {
	            throw new RuntimeException("Invalid file path:" + filePath);
	        }
        } catch (Exception e) {
            Log.e(TAG, "Write file(" + filePath + ") meets exception");
            e.printStackTrace();
        }
    }

    /**
     * M: For EVDO: check the sim is whether UIM or not.
     * @param subId the sim's sub id.
     * @return true: UIM; false: not UIM.
     */
    public static boolean isUSimType(int subId) {
        String phoneType = TelephonyManagerEx.getDefault().getIccCardType(subId);
        if (phoneType == null) {
            Log.d(TAG, "[isUIMType]: phoneType = null");
            return false;
        }
        Log.d(TAG, "[isUIMType]: phoneType = " + phoneType);
        return phoneType.equalsIgnoreCase("CSIM") || phoneType.equalsIgnoreCase("UIM")
            || phoneType.equalsIgnoreCase("RUIM");
    }

    /**
     * M: for check CSIM in gsm mode or not.
     * @param subId the CSIM's sub id.
     * @return true: in gsm; false: no.
     */
    public static boolean isCSIMInGsmMode(int subId) {
        if (isUSimType(subId)) {
            TelephonyManagerEx tmEx = TelephonyManagerEx.getDefault();
            int vnt = tmEx.getPhoneType(SubscriptionManager.getSlotId(subId));
            Log.d(TAG,
                "[isCSIMInGsmMode]:[NO_PHONE = 0; GSM_PHONE = 1; CDMA_PHONE = 2;]; phoneType:"
                    + vnt);
            if (vnt == TelephonyManager.PHONE_TYPE_GSM) {
                return true;
            }
        }
        return false;
    }

    public static int getDefaultDataSubId(Context context) {
        int subId = Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.MULTI_SIM_DATA_CALL_SUBSCRIPTION,
                -1);
                Log.i(TAG,"getDefaultDataSubId, value = " + subId);

        if(subId <= 0)
            subId = 1;

        return subId;
    }


}
