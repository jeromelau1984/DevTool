package com.lejia.devtool.download.logic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.lejia.devtool.download.DlListener;
import com.lejia.devtool.download.DlRemoteFile;

/**
 * 
 * @author jerome
 *
 */
public final class DlShFileRunnable extends BaseRunnable{

	public static final String TAG = DlShFileRunnable.class.getSimpleName();
	
	private String needDownloadFilemd5 = "";
	private String cfgVersion = "";
	private long remoteFileSize = -1;
	private String tempFileName = "";
	private String tempUrl = "";
	private String dlShFileName = "";
	
	protected DlShFileRunnable(DlListener listener){
		String pathStr = BASE_PATH;
		/*File baseFile = new File(pathStr);
		if(!baseFile.exists()){
			baseFile = Environment.getExternalStorageDirectory();
		}*/
		pathStr = pathStr + "/RemotExecFolder";
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
	
	protected void init(DlRemoteFile dlShFile){
		if(dlShFile == null || !dlShFile.isPrepare()){
			if(listener != null) {
				if(!TextUtils.isEmpty(dlShFileName) && !TextUtils.isEmpty(cfgVersion))
					listener.onDlFileError(TAG_INIT_ZIP, cfgVersion , dlShFileName);
			}	
			return;
		}
		
//		if(dlShFile.apkFiles == null || dlShFile.apkFiles.size() == 0){
//			if(listener != null){
//				if(!TextUtils.isEmpty(dlShFileName) && !TextUtils.isEmpty(cfgVersion))
//					listener.onDlFileError(TAG_INIT_APKFILES, cfgVersion , dlShFileName);
//			}
//			return;
//		}
		
		
		if(dlShFile.dlFileName.contains("/")){
			String tmpS[] = dlShFile.dlFileName.split("/");
			if(tmpS != null && tmpS.length >= 1){
				if(!TextUtils.isEmpty(tmpS[tmpS.length - 1]) && tmpS[tmpS.length - 1].contains(POSTFIX_SH))
					this.dlShFileName = tmpS[tmpS.length - 1].replace(POSTFIX_SH, "");
			}
		}else{
			if(!TextUtils.isEmpty(dlShFile.dlFileName) && dlShFile.dlFileName.contains(POSTFIX_SH)) 
				this.dlShFileName = dlShFile.dlFileName.replace(POSTFIX_SH, "");
			else
				this.dlShFileName = dlShFile.dlFileName;
		}	
		
		this.cfgVersion = dlShFile.cfgVersion;
		this.tempUrl = dlShFile.dlUrl;
		this.needDownloadFilemd5 = dlShFile.dlFileMd5;
		this.remoteFileSize = dlShFile.dlFileSize;

		Log.v(TAG, "dlShFile : " + dlShFile);
		if(TextUtils.isEmpty(mDownloadAbsolutePath)) mDownloadAbsolutePath = "/storage/sdcard0/RemotExecFolder";
		this.tempFileName = mDownloadAbsolutePath + "/" + dlShFileName + ".mc";
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
		Log.d(TAG, "downloadSh begin dlShFileName : " + dlShFileName);
		Log.d(TAG, "downloadSh begin tempUrl : " + tempUrl);
		Log.d(TAG, "downloadSh begin needDownloadFilemd5 : " + needDownloadFilemd5);
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
			Log.d(TAG, "downloadSh() URL = " + tempUrl);
			
			if (tempUrl != null && "".equals(tempUrl.trim())) {
				if(listener != null){
					if(!TextUtils.isEmpty(dlShFileName) && !TextUtils.isEmpty(cfgVersion))
						listener.onDlFileError(TAG_EMPTY_URL, cfgVersion , dlShFileName);
				}
				return DOWNLOAD_CONTINUE;
			}
//			if(!TextUtils.isEmpty(tempFileName)){
//				File existFile = new File(tempFileName.replace(".mc", POSTFIX_SH));
//				Log.i(TAG, " !!! NOTICE !!! existFile : " + existFile + " ; exists : " + existFile.exists());
//				if(existFile.exists() && !TextUtils.isEmpty(needDownloadFilemd5)){
//					Log.i(TAG, "==========================================");
//					Log.i(TAG, " !!! NOTICE !!! ");
//					Log.i(TAG, "existFile : " + existFile.getAbsolutePath());
//					Log.i(TAG, "MD5FileUtil.getFileMD5String(existFile) : " + MD5FileUtil.getMd5ByFile(existFile));
//					Log.i(TAG, "dlShFileName : " + dlShFileName);
//					Log.i(TAG, "needDownloadFilemd5 : " + needDownloadFilemd5);
//					Log.i(TAG, "==========================================");
//					if(TextUtils.equals(MD5FileUtil.getMd5ByFile(existFile), needDownloadFilemd5)){
//						Log.i(TAG, "The same zipFile md5 : " + existFile.getAbsolutePath());
//						transactionUnZip("existFile" , dlShFileName);
//						return DOWNLOAD_CONTINUE;
//					}
//				}
//			}
			
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
//					transactionUnZip("download code == 416" , dlShFileName);
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
				if(!TextUtils.isEmpty(dlShFileName) && !TextUtils.isEmpty(cfgVersion)) 
					listener.onProcess(dlShFileName , cfgVersion , (int) percent, usedTime , (int)remainSize);
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
							if(!TextUtils.isEmpty(dlShFileName) && !TextUtils.isEmpty(dlShFileName)) listener.onProcess(dlShFileName , cfgVersion , (int) percent, usedTime , (int)remainSize);
						}
						percent = tmpPercent;
					}
				}
			} while (length != DOWNLOAD_INTERRUPT && isRunning);
			isSuccess = true;

			if (isRunning && getDlShResult(tempUsedTime, isSuccess, resultData, file) == DOWNLOAD_INTERRUPT) {
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
		File reNameFile = new File(sb.append(dlShFileName).append(POSTFIX_SH).toString());
		if(!reNameFile.exists()){reNameFile.createNewFile();};
		boolean reNameSucc = file.renameTo(reNameFile);
		Log.i(TAG, "download percent 100% : " + reNameFile.getAbsoluteFile());
		if(reNameSucc){
			file.delete();
			if (null != listener){
				if(!TextUtils.isEmpty(dlShFileName) && !TextUtils.isEmpty(cfgVersion)) listener.onDownFinished(true, cfgVersion, dlShFileName);
			}
		}
	}

	private int getDlShResult(long tempUsedTime, boolean isSuccess,byte[] resultData, File file) throws IOException {
		Log.d(TAG, "getDlZipResult");
		endTime = System.currentTimeMillis();
		usedTime = tempUsedTime + (endTime - startTime);

		if (percent != 100) {
			Log.d(TAG, "percent step1 : " + percent);
			return DOWNLOAD_CONTINUE;
		} else if (percent == 100) {
			waiting();
			reNameTmpFile("download percent 100%", file);
//			transactionUnZip("download percent 100%" , dlShFileName);
			return DOWNLOAD_INTERRUPT;
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
			if(!TextUtils.isEmpty(cfgVersion) && !TextUtils.isEmpty(dlShFileName)) listener.onDlFileError(code, cfgVersion , dlShFileName);
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
	
//	private Semaphore unZipLock = new Semaphore(1);
//	protected boolean isUnZipLockFair(){
//		return unZipLock.isFair();
//	}
	
	/**
     * unzip
     * @param zipfile
     * @param descDir
     * @throws IOException
     * @throws ZipException
     */
	private static void unZipFiles(File zipfile, String descDir) throws ZipException, IOException {
		String zipEntryName;
		@SuppressWarnings("resource")
		ZipFile zf = new ZipFile(zipfile);
		for (Enumeration<?> entries = zf.entries(); entries.hasMoreElements();) {
			ZipEntry entry = (ZipEntry) entries.nextElement();
			zipEntryName = entry.getName();
			InputStream in = zf.getInputStream(entry);
			OutputStream out = new FileOutputStream(descDir + zipEntryName);
			byte[] buf1 = new byte[1024];
			int len;
			while ((len = in.read(buf1)) > 0) {
				out.write(buf1, 0, len);
			}
			in.close();
			out.close();
			Log.i(TAG, "unZip finished : " + zipEntryName);
		}
	}
	
	/**
	 * 
	 * @param tag
	 * @param fileName
	 * @throws IOException
	 */
//	private void transactionUnZip(String tag , String fileName) throws IOException {
//		try {
//			unZipLock.acquire();
//			
//			StringBuffer sb = new StringBuffer(BASE_PATH).append(DL_FILE);
//			File reNameFile = new File(sb.append(fileName).append(POSTFIX_SH).toString());
//			if (FileUtil.isZipFile(reNameFile.getAbsoluteFile()) && TextUtils.equals(needDownloadFilemd5,MD5FileUtil.getMd5ByFile(reNameFile))) {
//				Log.i(TAG, tag + "step1");
//				StringBuffer sb1 = new StringBuffer(BASE_PATH).append(DL_FILE);
//				File destFile = new File(sb1.toString());
//				Log.i(TAG, tag + " step2 destFile.exists : " + destFile.exists());
//				Log.i(TAG, tag + " step2 destFile.isDirectory : " + destFile.isDirectory());
//				Log.i(TAG, tag + " step2 destFile.getAbsolutePath : " + destFile.getAbsolutePath());
//				if (destFile.exists()) {
//					unZipFiles(reNameFile, sb1.toString());
//					Log.i(TAG, tag + " step3 : " + destFile.isDirectory());
//					Log.i(TAG, tag + " step4 : " + destFile.getAbsolutePath());
//					File[] subFile = destFile.listFiles();
//					Log.i(TAG, tag + " step5 : " + subFile.length);
//					if (null == subFile || subFile.length == 0) {
//						unZipLock.release();
//						if (null != listener){
//							if(!TextUtils.isEmpty(dlShFileName) && !TextUtils.isEmpty(cfgVersion)) listener.onDlFileError(TAG_TRANS_UNZIP , cfgVersion, dlShFileName);
//						}
//						return;
//					}
//
//					if (apkFiles != null && apkFiles.size() > 0) {
//						int md5EqualCount = 0;
//						Iterator<Entry<String, DlApk>> iter = apkFiles.entrySet().iterator();
//						while (iter.hasNext()) {
//							Entry<String, DlApk> entry = iter.next();
//							String namePart = entry.getKey();
//							File tmpMd5File = new File(BASE_PATH + DL_FILE + namePart + POSTFIX_APK);
//							Log.i(TAG, tag + " step6 tmpMd5File : " + tmpMd5File.getAbsolutePath());
//							Log.i(TAG, tag + " step6 : " + tmpMd5File.exists());
//							if (tmpMd5File.exists()) {
//								DlApk apk = entry.getValue();
//								Log.i(TAG, tag + " step7 : " + apk);
//								if (null != apk && !TextUtils.isEmpty(apk.md5)) {
//									Log.i(TAG, tag + " step8 MD5FileUtil.getMd5ByFile(tmpMd5File) : " + MD5FileUtil.getMd5ByFile(tmpMd5File));
//									Log.i(TAG, tag + " step8 apk.md5 : " + apk.md5);
//									if (TextUtils.equals(MD5FileUtil.getMd5ByFile(tmpMd5File),apk.md5)) {
//										Log.i(TAG, tag + " step9");
//										md5EqualCount++;
//									}
//								}
//							}
//						}
//						Log.i(TAG, tag + " getMd5ByFile md5EqualCount : " + md5EqualCount);
//						if (md5EqualCount == apkFiles.size()) {
//							unZipLock.release();
//							if (null != listener){
//								if(!TextUtils.isEmpty(dlShFileName) && !TextUtils.isEmpty(cfgVersion))
//									listener.onUnzipFinished(true, cfgVersion,dlShFileName);
//							} 
//							return;
//						} else {
//							unZipLock.release();
//							if (null != listener){
//								if(!TextUtils.isEmpty(dlShFileName) && !TextUtils.isEmpty(cfgVersion))
//									listener.onDlFileError(TAG_SIZE_NQ , cfgVersion,dlShFileName);
//							}
//							return;
//						}
//					}
//				}
//			}
//		} catch (InterruptedException e) {
//			Log.e(TAG, tag + " InterruptedException : " + e.toString());
//		} finally {
//			unZipLock.release();
//			if(null != listener) {
//				if(!TextUtils.isEmpty(dlShFileName) && !TextUtils.isEmpty(cfgVersion))
//					listener.onDlFileError(TAG_LOCK_RELEASE , cfgVersion , dlShFileName);
//			}
//		}
//	}

	@Override
	protected void transact(BaseTransact btact) {
		
	}

	@Override
	protected int download(BaseRunnable runnable, BaseTransact trans) {
		return 0;
	}

}
