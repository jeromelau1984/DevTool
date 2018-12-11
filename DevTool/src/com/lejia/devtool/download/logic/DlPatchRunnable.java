package com.lejia.devtool.download.logic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.lejia.devtool.download.DlListener;
import com.lejia.devtool.download.DlRemoteFile;
import com.lejia.devtool.download.MD5FileUtil;

/**
 * 
 * @author jerome
 *
 */
public final class DlPatchRunnable extends BaseRunnable{

	public static final String TAG = DlPatchRunnable.class.getSimpleName();
	
	private String needDownloadFilemd5 = "";
	private String cfgVersion = "";
	private long remoteFileSize = -1;
	private String tempFileName = "";
	private String tempUrl = "";
	private String dlPatchName = "";
	
	private String nativeFileMd5 = "";
	private String newFileMd5 = "";
	
	protected DlPatchRunnable(DlListener listener){
		String pathStr = BASE_PATH;
		File baseFile = new File(pathStr);
		if(!baseFile.exists()){
			baseFile = Environment.getExternalStorageDirectory();
		}
		pathStr = pathStr + "/carrobotUpgrade";
		File dirFile = new File(pathStr);
		if (!(dirFile.exists()) || !(dirFile.isDirectory())) {
			dirFile.mkdirs();
		}
		File file = new File(pathStr);
		if (!file.exists()) {
			try {
				boolean isSuccess = file.mkdirs();
				if (!isSuccess) {
					Log.d(TAG, "mkdirs failed");
				}
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			}
		}
		this.mDownloadAbsolutePath = file.getAbsolutePath();
		this.listener = listener;
	}
	
	protected void init(DlRemoteFile upgradePatch){
		if(upgradePatch == null || !upgradePatch.isPrepare()){
			if(listener != null) {
				if(!TextUtils.isEmpty(dlPatchName) && !TextUtils.isEmpty(cfgVersion))
					listener.onDlFileError(TAG_INIT_ZIP, cfgVersion , dlPatchName);
			}	
			return;
		}
		
//		if(upgradeZip.apkFiles == null || upgradeZip.apkFiles.size() == 0){
//			if(listener != null){
//				if(!TextUtils.isEmpty(dlPatchName) && !TextUtils.isEmpty(cfgVersion))
//					listener.onDlFileError(TAG_INIT_APKFILES, cfgVersion , dlPatchName);
//			}
//			return;
//		}
		Log.v(TAG, "upgradePatch.dlFileName : " + upgradePatch.dlFileName);
		if(upgradePatch.dlFileName.contains("/")){
			String tmpS[] = upgradePatch.dlFileName.split("/");
			if(tmpS != null && tmpS.length >= 1){
				if(!TextUtils.isEmpty(tmpS[tmpS.length - 1]) && tmpS[tmpS.length - 1].contains(POSTFIX_PATCH))
					this.dlPatchName = tmpS[tmpS.length - 1].replace(POSTFIX_PATCH, "");
			}
		}else{
			if(!TextUtils.isEmpty(upgradePatch.dlFileName) && upgradePatch.dlFileName.contains(POSTFIX_PATCH)) 
				this.dlPatchName = upgradePatch.dlFileName.replace(POSTFIX_PATCH, "");
		}
		Log.v(TAG, "this.dlPatchName : " + this.dlPatchName);
			
		this.cfgVersion = upgradePatch.cfgVersion;
		this.tempUrl = upgradePatch.dlUrl;
		this.needDownloadFilemd5 = upgradePatch.dlFileMd5;
		this.newFileMd5 = upgradePatch.patchFinishFileMd5;
		this.nativeFileMd5 = upgradePatch.nativeFileMd5;
		this.remoteFileSize = upgradePatch.dlFileSize;

		Log.v(TAG, "upgradePatch : " + upgradePatch);
		if(TextUtils.isEmpty(mDownloadAbsolutePath)) mDownloadAbsolutePath = "/storage/sdcard0/carrobotUpgrade";
		this.tempFileName = mDownloadAbsolutePath + "/" + dlPatchName + ".patchmc";
		Log.v(TAG, "tempFileName :: " + tempFileName);
		prepare();
	}
	
	public void run() {
		super.run();
	}

	protected int download() {
		if (!isRunning) {
			return DOWNLOAD_CONTINUE;
		}
		Log.d(TAG, "downloadPatch begin dlPatchName : " + dlPatchName);
		Log.d(TAG, "downloadPatch begin tempUrl : " + tempUrl);
		Log.d(TAG, "downloadPatch begin needDownloadFilemd5 : " + needDownloadFilemd5);
		boolean isSuccess = false;

		byte[] resultData = new byte[0];

		File file = null;
		FileOutputStream outputStream = null;
		InputStream is = null;
		HttpURLConnection conn = null;
		URL myURL = null;

		long tempUsedTime = usedTime;
		long remainSize = -1;
		try {
			Log.d(TAG, "downloadPatch() URL = " + tempUrl);
			
			if (tempUrl != null && "".equals(tempUrl.trim())) {
				if(listener != null){
					if(!TextUtils.isEmpty(dlPatchName) && !TextUtils.isEmpty(cfgVersion))
						listener.onDlFileError(TAG_EMPTY_URL, cfgVersion , dlPatchName);
				}
				return DOWNLOAD_CONTINUE;
			}
			if(!TextUtils.isEmpty(tempFileName)){
				File existFile = new File(tempFileName.replace(".patchmc", POSTFIX_PATCH));
				Log.i(TAG, " !!! NOTICE !!! existFile : " + existFile + " ; exists : " + existFile.exists());
				if(existFile.exists() && !TextUtils.isEmpty(needDownloadFilemd5)){
					Log.i(TAG, "==========================================");
					Log.i(TAG, " !!! NOTICE !!! ");
					Log.i(TAG, "existFile : " + existFile.getAbsolutePath());
					Log.i(TAG, "MD5FileUtil.getFileMD5String(existFile) : " + MD5FileUtil.getMd5ByFile(existFile));
					Log.i(TAG, "dlPatchName : " + dlPatchName);
					Log.i(TAG, "needDownloadFilemd5 : " + needDownloadFilemd5);
					Log.i(TAG, "==========================================");
					if(TextUtils.equals(MD5FileUtil.getMd5ByFile(existFile), needDownloadFilemd5)){
						Log.i(TAG, "The same zipFile md5 : " + existFile.getAbsolutePath());
						transactionPatch("existFile" , dlPatchName);
						return DOWNLOAD_CONTINUE;
					}
				}
			}
			
			myURL = new URL(tempUrl);
			conn = (HttpURLConnection) myURL.openConnection();

			conn.setConnectTimeout(5 * 1000);
			conn.setRequestMethod("GET");
			conn.setRequestProperty(
					"Accept",
					"image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
			conn.setRequestProperty("Accept-Language", "zh-CN");
			conn.setRequestProperty(
					"User-Agent",
					"Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
			conn.setRequestProperty("Connection", "Keep-Alive");

			long fileSize = 0;

			file = new File(tempFileName);
			Log.i(TAG, "tempFileName : " + tempFileName + "  is exists : " + file.exists());
			if (file.exists()) {
				fileSize = file.length();
				Log.i(TAG, "remoteFileSize : " + remoteFileSize + " ; fileSize : " + fileSize);
				/**
				 * download finished , reboot without rename
				 */
				if(remoteFileSize == fileSize){
					waiting();
					reNameTmpFile("download code == 416", file);
					transactionPatch("download code == 416" , dlPatchName);
					return DOWNLOAD_CONTINUE;
				}
				
				outputStream = new FileOutputStream(file, true);
				conn.setRequestProperty("RANGE", "bytes=" + fileSize + "-");
			} else {
				boolean bo = file.createNewFile();
				if (!bo) {
					Log.d(TAG, "create new file false");
				}
				outputStream = new FileOutputStream(file, true);
			}
			int code = conn.getResponseCode();
			
			String mAcceptRanges = conn.getHeaderField("Accept-Ranges");
			Log.i(TAG, "Accept-Ranges : " + mAcceptRanges);
			if(!TextUtils.isEmpty(mAcceptRanges) && TextUtils.equals(mAcceptRanges, "none")){
				if(file.exists() && fileSize > 0){
					file.delete();
					boolean bo = file.createNewFile();
					if (!bo) {
						Log.d(TAG, "do not support breakpoint , del the same name tmpFile , create new file false");
					}
				}
			}
			
			Log.d(TAG, "getRequestProperty() = " + conn.getRequestProperty("RANGE"));
			Log.d(TAG, "getResponseCode() = " + code);
			
			/**
			 * do not support breakpoint , del the same name tmpFile , reload
			 */
//			if(code == 200 && file.exists() && fileSize > 0){
//				file.delete();
//				boolean bo = file.createNewFile();
//				if (!bo) {
//					Log.d(TAG, "do not support breakpoint , del the same name tmpFile , create new file false");
//				}
//			}
			
			if (code != 200 && code != 206) {
				return errorCode(conn, code);
			}
			is = conn.getInputStream();
			remainSize = conn.getContentLength();

			Log.d(TAG, "conn.getContentLength() = " + conn.getContentLength());
			long tmpSize = getSize(conn, remainSize);
			if(tmpSize < 0){
				return errorCode(conn, -100);
			}
			long index = fileSize;
			if (remainSize > 0) {
				percent = fileSize * 100 / (remainSize + fileSize);
				fileSize = remainSize + fileSize;
			}

			long tmpPercent = 0;
			if (listener != null) {
				if(!TextUtils.isEmpty(dlPatchName) && !TextUtils.isEmpty(cfgVersion)) 
					listener.onProcess(dlPatchName , cfgVersion , (int) percent, usedTime , (int)remainSize);
			}
			
			int chunkSize = 512;

			byte[] data = new byte[chunkSize];
			int length = 0;
			long tempFileSize = fileSize;

			do {
				length = is.read(data, 0, chunkSize);
				if (length != -1) {
					outputStream.write(data, 0, length);
					outputStream.flush();
					
					if (tempFileSize > 0) {
						index += length;
						tmpPercent = index * 100 / tempFileSize;

						if (tmpPercent != percent) {
							if(!TextUtils.isEmpty(dlPatchName) && !TextUtils.isEmpty(dlPatchName)) listener.onProcess(dlPatchName , cfgVersion , (int) percent, usedTime , (int)remainSize);
						}
						percent = tmpPercent;
					}
				}
			} while (length != DOWNLOAD_INTERRUPT && isRunning);
			isSuccess = true;

			if (isRunning && getDlZipResult(tempUsedTime, isSuccess, resultData, file) == DOWNLOAD_INTERRUPT) {
				return DOWNLOAD_CONTINUE;
			}

		} catch (Exception e) {
			Log.e(TAG, "errorCode Exception : " + e.getMessage());
			return errorCode(conn, -101);
		} finally {
			file = null;
			try {
				if (outputStream != null) {
					outputStream.close();
					outputStream = null;
				}
				if (is != null) {
					is.close();
					is = null;
				}
				if (conn != null) {
					conn.disconnect();
					conn = null;
				}
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}
		}
		return DOWNLOAD_CONTINUE;
	}
	
	private void reNameTmpFile(String tag , File file) throws IOException{
		StringBuffer sb = new StringBuffer(BASE_PATH).append(DL_FILE);
		File reNameFile = new File(sb.append(dlPatchName).append(POSTFIX_PATCH).toString());
		if(!reNameFile.exists()){reNameFile.createNewFile();};
		boolean reNameSucc = file.renameTo(reNameFile);
		Log.i(TAG, "download percent 100% : " + reNameFile.getAbsoluteFile());
		if(reNameSucc) file.delete();
	}

	private int getDlZipResult(long tempUsedTime, boolean isSuccess,byte[] resultData, File file) throws IOException {
		Log.d(TAG, "getDlZipResult");
		endTime = System.currentTimeMillis();
		usedTime = tempUsedTime + (endTime - startTime);

		if (percent != 100) {
			Log.d(TAG, "percent step1 : " + percent);
			return DOWNLOAD_CONTINUE;
		} else if (percent == 100) {
			waiting();
			reNameTmpFile("download percent 100%", file);
			transactionPatch("download percent 100%" , dlPatchName);
		}
		File f = new File(tempFileName);
		if (!f.exists()) {
			percent = 0;
			Log.d(TAG, "===========================================");
			Log.d(TAG, "getDlZipResult tempFileName  : " + tempFileName + " f.exists() : " + f.exists());
			Log.d(TAG, "===========================================");
			return DOWNLOAD_CONTINUE;
		}
		return DOWNLOAD_CONTINUE;
	}

	/**
	 *  dispose errorCode
	 * 
	 * @param conn	HttpURLConnection
	 * @param code	int
	 * @return int
	 */
	private int errorCode(HttpURLConnection conn, int code) {

		endTime = System.currentTimeMillis();
		long time = endTime - startTime;
		usedTime += time;
		if (listener != null) {
			if(!TextUtils.isEmpty(cfgVersion) && !TextUtils.isEmpty(dlPatchName)) listener.onDlFileError(code, cfgVersion , dlPatchName);
		}
		return DOWNLOAD_CONTINUE;
	}

	/**
	 * if there is no contentSize , get it by Content-Range
	 * 
	 * @param conn	HttpURLConnection
	 * @param remainSize	long
	 */
	private long getSize(HttpURLConnection conn, long remainSize) {
		if (remainSize < 0) {
			String contentRange = conn.getHeaderField("Content-Range");
			if (null != contentRange) {
				// Content-Range: bytes 1230319-3098402/3098403
				int index = contentRange.indexOf('/');
				if (index != -1) {
					String length = contentRange.substring(index + 1,
							contentRange.length());
					Log.e(TAG, "length = " + length);
					try {
						remainSize = Long.parseLong(length);
					} catch (Exception e) {
						Log.e(TAG, e.toString());
						errorCode(conn, -102);
					}
				}else{
					errorCode(conn, -103);
				}
			}
		}
		return remainSize;
	}
	
//	/**
//     * unzip
//     * @param zipfile
//     * @param descDir
//     * @throws IOException
//     * @throws ZipException
//     */
//	private static void unZipFiles(File zipfile, String descDir) throws ZipException, IOException {
//		String zipEntryName;
//		ZipFile zf = new ZipFile(zipfile);
//		for (Enumeration<? extends ZipEntry> entries = zf.entries(); entries.hasMoreElements();) {
//			ZipEntry entry = (ZipEntry) entries.nextElement();
//			zipEntryName = entry.getName();
//			InputStream in = zf.getInputStream(entry);
//			OutputStream out = new FileOutputStream(descDir + zipEntryName);
//			byte[] buf1 = new byte[1024];
//			int len;
//			while ((len = in.read(buf1)) > 0) {
//				out.write(buf1, 0, len);
//			}
//			in.close();
//			out.close();
//			Log.i(TAG, "unZip finished : " + zipEntryName);
//		}
//	}
	
	/**
	 * 
	 * @param tag
	 * @param fileName
	 * @throws IOException
	 */
	private void transactionPatch(String tag , String fileName) throws IOException {
		try {
			unZipLock.acquire();
			
			StringBuffer sb = new StringBuffer(BASE_PATH).append(DL_FILE);
			File reNameFile = new File(sb.append(fileName).append(POSTFIX_PATCH).toString());
			String nativeFileName = BASE_PATH + DL_FILE + DlFileHelper.getNativeUpFilePath();
			String newFileName = reNameFile.getAbsolutePath().replace(".patch", ".zip");
			
			Log.i(TAG, "transactionPatch nativeFileName : " + nativeFileName);
			Log.i(TAG, "transactionPatch cfgVersion : " + cfgVersion);
			Log.i(TAG, "transactionPatch reNameFile.getAbsolutePath() : " + reNameFile.getAbsolutePath());
			Log.i(TAG, "transactionPatch newFileName : " + newFileName);
			
			//FIXME do patch finished, equals md5, if matches send broadcast
			if(!TextUtils.isEmpty(nativeFileName) && !TextUtils.isEmpty(cfgVersion) 
					&& !TextUtils.isEmpty(reNameFile.getAbsolutePath()) && !TextUtils.isEmpty(newFileName)){
				
				newFileName = BASE_PATH + DL_FILE + reNameFile.getName().replace(POSTFIX_PATCH, POSTFIX_ZIP);
				/*int patchRlt = PatchUtils.patch(nativeFileName, newFileName , reNameFile.getAbsolutePath());
				Log.i(TAG, "PatchUtils.patch patchRlt : " + patchRlt);
				
				if(patchRlt != 0){
					if(null != listener && !TextUtils.isEmpty(cfgVersion) && !TextUtils.isEmpty(dlPatchName)) 
						listener.onDlFileError(TAG_PATCH_FAILED, cfgVersion , dlPatchName);
					unZipLock.release();
					return;
				}
				*/
				
				Log.i(TAG, "==========================================");
				Log.i(TAG, " !!! NOTICE !!! ");
				//Log.i(TAG, "PatchUtils.patch patchRlt : " + patchRlt);
				Log.i(TAG, "PatchUtils.patch patchRlt  newFileMd5 : " + newFileMd5);
				Log.i(TAG, "PatchUtils.patch patchRlt  MD5FileUtil.getMd5ByFile(new File(newFileName)) : " + MD5FileUtil.getMd5ByFile(new File(newFileName)));
				
				Log.i(TAG, "PatchUtils.patch patchRlt  needDownloadFilemd5 : " + needDownloadFilemd5);
				Log.i(TAG, "PatchUtils.patch patchRlt  MD5FileUtil.getMd5ByFile(reNameFile) : " + MD5FileUtil.getMd5ByFile(reNameFile));
				
				Log.i(TAG, "PatchUtils.patch patchRlt  nativeFileMd5 : " + nativeFileMd5);
				Log.i(TAG, "PatchUtils.patch patchRlt  MD5FileUtil.getMd5ByFile(new File(nativeFileName)) : " + MD5FileUtil.getMd5ByFile(new File(nativeFileName)));
				Log.i(TAG, "==========================================");
				
				if(TextUtils.equals(MD5FileUtil.getMd5ByFile(new File(newFileName)),newFileMd5)
						&& TextUtils.equals(MD5FileUtil.getMd5ByFile(reNameFile), needDownloadFilemd5)
						&& TextUtils.equals(MD5FileUtil.getMd5ByFile(new File(nativeFileName)), nativeFileMd5)){
					unZipLock.release();
					
					/*del nativeFile&patchFile*/
					File nativeFile = new File(nativeFileName);
					boolean delNativeFileSucc = false;
					
					Log.i(TAG, "PatchUtils.patch delNativeFileSucc : " + delNativeFileSucc);
					
					if(nativeFile.exists()){
						delNativeFileSucc = nativeFile.delete();
					}else{
						Log.e(TAG, "delNativeFileSucc : " + delNativeFileSucc);
					}
					boolean delPatchFileSucc = false;
					if(delNativeFileSucc && reNameFile.exists()){
						delPatchFileSucc = reNameFile.delete();
					}else{
						Log.e(TAG, "delPatchFileSucc : " + delPatchFileSucc);
					}
					
					if(null != listener && !TextUtils.isEmpty(cfgVersion)
						&& !TextUtils.isEmpty(nativeFileName)
						&& !TextUtils.isEmpty(reNameFile.getAbsolutePath())
						&& !TextUtils.isEmpty(newFileName)){
						listener.onPatchFinished(true, cfgVersion,nativeFileName, reNameFile.getAbsolutePath(), newFileName);
					}
				}else{
					/*md5 not matches*/
					File nativeFile = new File(nativeFileName);
					if(nativeFile.exists() && !TextUtils.equals(MD5FileUtil.getMd5ByFile(nativeFile), nativeFileMd5)){
						boolean delNativeFile = nativeFile.delete();
						Log.e(TAG, "md5 not matches delNativeFile : " + delNativeFile);
					}
					if(reNameFile.exists() && !TextUtils.equals(MD5FileUtil.getMd5ByFile(reNameFile), needDownloadFilemd5)){
						boolean delPatchFile = reNameFile.delete();
						Log.e(TAG, "md5 not matches delPatchFile : " + delPatchFile);
					}
					File newFile = new File(newFileName);
					if(newFile.exists() && !TextUtils.equals(MD5FileUtil.getMd5ByFile(newFile), newFileMd5)){
						boolean delNewFile = nativeFile.delete();
						Log.e(TAG, "md5 not matches delNewFile : " + delNewFile);
					}
					
					unZipLock.release();
					if(null != listener && !TextUtils.isEmpty(cfgVersion) && !TextUtils.isEmpty(dlPatchName)) 
						listener.onDlFileError(TAG_FILE_MD5_MATCH, cfgVersion , dlPatchName);
					return;
				}
			}else{
				Log.e(TAG, "transactionPatch flag");
				unZipLock.release();
				if(null != listener && !TextUtils.isEmpty(cfgVersion) && !TextUtils.isEmpty(dlPatchName)) 
					listener.onDlFileError(TAG_FILE_ABSPATH_EMPTY, cfgVersion , dlPatchName);
				return;
			}
		} catch (InterruptedException e) {
			Log.e(TAG, tag + " InterruptedException : " + e.toString());
		} finally {
			unZipLock.release();
			if(null != listener) 
				if(!TextUtils.isEmpty(dlPatchName) && !TextUtils.isEmpty(cfgVersion))
					listener.onDlFileError(TAG_LOCK_RELEASE , cfgVersion , dlPatchName);
		}
	}

	@Override
	protected void transact(BaseTransact btact) {
		
	}

	@Override
	protected int download(BaseRunnable runnable, BaseTransact trans) {
		return 0;
	}
	/*
	static {
		try {
			System.loadLibrary("lejiapatch");
		} catch (java.lang.UnsatisfiedLinkError e) {
			Log.e(TAG, "System.loadLibrary(lejiapatch) : " + e.toString());
		}
	}
	*/
}
