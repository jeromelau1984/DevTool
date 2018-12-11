package com.lejia.devtool.download;

import java.util.HashMap;

import android.text.TextUtils;

public class DlRemoteFile extends BaseBean{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6203280398824443285L;
	
	public String dlFileName = null;
	public String cfgVersion = null;
	public String dlUrl = null;
	public long dlFileSize = -1;
	public String dlFileMd5 = null;
	
	public String nativeFileMd5 = null;
	public String patchFinishFileMd5 = null; 
	
	/*Changes in demand can adjust the Value*/
	public HashMap<String,DlApk> apkFiles = new HashMap<String,DlApk>(5);
	
	public DlRemoteFile(){
		
	}
	
	public boolean isPrepare(){
		return (!TextUtils.isEmpty(dlFileName)) 
						&& (!TextUtils.isEmpty(dlFileMd5))
						&& (!TextUtils.isEmpty(cfgVersion));
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null)
			return false;
		if(!(o instanceof DlRemoteFile))
			return false;
		final DlRemoteFile qb = (DlRemoteFile) o;
		if(!TextUtils.equals(dlFileMd5, qb.dlFileMd5)){
			return false;
		}
		if(!dlFileName.equals(qb.dlFileName)){
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result = dlFileMd5.hashCode();
		result = 29*result + dlFileMd5.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "UpgradeZip [dlFileName=" + dlFileName + ", cfgVersion="
				+ cfgVersion + ", dlUrl=" + dlUrl + ", dlFileSize="
				+ dlFileSize + ", dlFileMd5=" + dlFileMd5 + ", nativeFileMd5="
				+ nativeFileMd5 + ", patchFinishFileMd5=" + patchFinishFileMd5
				+ ", apkFiles=" + apkFiles + "]";
	}

}
