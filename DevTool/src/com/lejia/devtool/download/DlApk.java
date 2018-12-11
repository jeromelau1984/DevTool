package com.lejia.devtool.download;

import java.io.Serializable;

import android.text.TextUtils;

public class DlApk implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6203280398824443285L;
	
	public String pkgName = null;
	public String appName = null;
	public String version = null;
	public String url = null;
	public long size = -1;
	public String md5 = null;
	public boolean isUpdate = true;
	
	public DlApk(){
		
	}
	
	public DlApk(String pkgName, String appName) {
		super();
		this.pkgName = pkgName;
		this.appName = appName;
	}

	public DlApk(String pkgName, String appName, boolean isUpdate) {
		super();
		this.pkgName = pkgName;
		this.appName = appName;
		this.isUpdate = isUpdate;
	}
	
	public boolean isPrepare(){
		return (!TextUtils.isEmpty(pkgName)) 
						&& (!TextUtils.isEmpty(appName)) 
						&& (!TextUtils.isEmpty(md5))
						&& (!TextUtils.isEmpty(version));
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;
		if(o == null)
			return false;
		if(!(o instanceof DlApk))
			return false;
		final DlApk qb = (DlApk) o;
		if(!TextUtils.equals(md5, qb.md5)){
			return false;
		}
		if(!pkgName.equals(qb.pkgName)){
			return false;
		}
		if(!appName.equals(qb.appName)){
			return false;
		}
		if(!version.equals(qb.version)){
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result = pkgName.hashCode();
		result = 29*result + appName.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "UpgradeApk [pkgName=" + pkgName + ", appName=" + appName
				+ ", version=" + version + ", url=" + url + ", size=" + size
				+ ", md5=" + md5 + ", isUpdate=" + isUpdate + "]";
	}

}
