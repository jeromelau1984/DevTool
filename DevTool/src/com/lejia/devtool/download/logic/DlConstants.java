package com.lejia.devtool.download.logic;

public interface DlConstants {
	
//	static final String DOWNLOAD_URL = "http://wx.lejia.cn/hud_log/";
	static final String DOWNLOAD_URL = "http://test.lejia.cn/hud_log/";
	
	static final String ERROR_INIT_CONFIG_WITH_NULL = "DlFileHelper configuration can not be initialized with null";

	static final String DL_FILE = "/RemotExecFolder/";
	static final String POSTFIX_ZIP = ".zip";
	static final String POSTFIX_PATCH = ".patch";
	static final String POSTFIX_APK = ".lejia";
	static final String POSTFIX_SH = ".sh";
	static final String BASE_PATH = "/storage/sdcard0";
	
	final int DOWNLOAD_CONTINUE = 100;
	final int DOWNLOAD_INTERRUPT = -1;
	
	static final int TAG_INIT_ZIP = 1<<0;
	static final int TAG_INIT_APKFILES = 1<<1;
	static final int TAG_EMPTY_URL = 1<<2;
	static final int TAG_TRANS_UNZIP = 1<< 3;
	static final int TAG_SIZE_NQ = 1<<4;
	static final int TAG_LOCK_RELEASE = 1<<5;
	static final int TAG_DEL_FILE = 1<<6;
	static final int TAG_FILE_MD5_MATCH = 1<<7;
	static final int TAG_FILE_ABSPATH_EMPTY = 1<<8;
	static final int TAG_PATCH_FAILED = 1<<9;
	
	static final String PKG_NAME_LAUNCHER = "com.lejia.carrobot";
	static final String PKG_NAME_AI_SERVER = "com.aispeech.aiserver";
	static final String PKG_NAME_GESTURE = "com.lejia.gesture";
	static final String PKG_NAME_BLUETOOTHEXT = "com.lejia.bluetoothext";
	static final String PKG_NAME_CONMANAGER = "com.aispeech.conmanager";
	
	static final String ACTION_SEND_UP_BROADCAST = "android.lejia.upgrade.mvsystemapp";
	
}
