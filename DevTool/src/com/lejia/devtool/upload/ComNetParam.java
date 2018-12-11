package com.lejia.devtool.upload;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
public class ComNetParam {

    private static final String SN_TAG = "sn";
    private static final String SIGN_TAG = "sign";
    private static final String TS_TAG = "ts";
    private static final String HD_TAG = "hd";
    private static final String RM_TAG = "rm";

    private static final String SECRET_KEY = "##ileja_2015##";
    @SuppressLint("InlinedApi")
	private static final String VALUE_SN = android.os.Build.SERIAL;

    public static String getCommonParam(){
        StringBuffer sb = new StringBuffer();

        //sign
        long tm = System.currentTimeMillis();
        String sign = null;
        try {
            sign = getSign(tm);
        }catch (NoSuchAlgorithmException e){

        }
        if(TextUtils.isEmpty(sign)==false){
            sb.append(SIGN_TAG).append("=").append(sign);
        }

        //sn
        sb.append("&").append(SN_TAG).append("=").append(VALUE_SN);
        //tm
        sb.append("&").append(TS_TAG).append("=").append(tm);

        //hardversion
        String hd = getSystemConf("ro.hardware");
        if(TextUtils.isEmpty(hd)==false){
            sb.append("&").append(HD_TAG).append("=").append(hd);
        }

        //romversion
        String rm = getSystemConf("ro.mediatek.version.release");
        if(TextUtils.isEmpty(rm)==false){
            sb.append("&").append(RM_TAG).append("=").append(rm);
        }

        return sb.toString();
    }

    public static String getSign(long timestamp) throws NoSuchAlgorithmException {
        String sigValue = VALUE_SN + "_" + timestamp + "_" + SECRET_KEY;
        String sign = Sha1Util.sha1(sigValue);

        return sign;
    }
    
	private static String getSystemConf(String configName){
		try {
			Process process = Runtime.getRuntime().exec("getprop "+configName);
			InputStreamReader ir = new InputStreamReader(process.getInputStream());
			BufferedReader input = new BufferedReader(ir);
			String value = input.readLine();
			input.close();
			ir.close();
			process.destroy();
			return value;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

}
