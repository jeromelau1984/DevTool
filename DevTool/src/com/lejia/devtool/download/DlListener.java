package com.lejia.devtool.download;

public interface DlListener{
	
	/**
	 * 
	 * @param result
	 * @param pkgName
	 * @param appName
	 */
    void onDownFinished(boolean result, String pkgName , String appName);
    
    /**
     * 
     * @param pkgName
     * @param appName
     * @param percent
     * @param usedTime
     * @param pkg_size
     */
    void onProcess(String pkgName , String appName , int percent, long usedTime , int pkg_size);
    
    /**
     * 
     * @param errorCode
     * @param pkgName
     * @param appName
     */
    void onError(int errorCode , String pkgName , String appName);
    
    void onUnzipFinished(boolean isUnzipSucc, String cfgVersion , String zipName);
	void onDlFileError(int status , String cfgVersion , String fileName);
	
	void onPatchFinished(boolean isPatchSucc, String cfgVersion , String nativeFileName , String patchFileName, String newFileName);
}
