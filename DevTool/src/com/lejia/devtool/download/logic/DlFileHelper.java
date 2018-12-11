package com.lejia.devtool.download.logic;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.lejia.devtool.download.DlApk;
import com.lejia.devtool.download.DlListener;
import com.lejia.devtool.download.DlQueue;
import com.lejia.devtool.download.DlRemoteFile;
import com.lejia.devtool.download.NetUtil;
import com.lejia.devtool.download.ThreadPoolManager;
import com.lejia.devtool.LauncherApplication;
import com.lejia.devtool.Utils;

public class DlFileHelper implements DlConstants {

	private static final String TAG = DlFileHelper.class.getSimpleName();

	private DlQueue mQueue;

	private DlFileHelper() {
	}

	private static DlFileHelper instance = null;

	private DlFileHelperConfig configuration;

	public static DlFileHelper getIns() {
		if (instance == null) {
			synchronized (DlFileHelper.class) {
				if (instance == null) {
					instance = new DlFileHelper();
				}
			}
		}
		return instance;
	}

	public synchronized void init(DlFileHelperConfig configuration) {
		if (configuration == null) {
			throw new IllegalArgumentException(ERROR_INIT_CONFIG_WITH_NULL);
		}
		if (this.configuration == null) {
			this.configuration = configuration;
		} else {
			// TODO
		}
	}

	public static String getPkgVersion(String pkgName) {
		if (TextUtils.isEmpty(pkgName)) {
			throw new IllegalArgumentException(
					"packageName must not be null...");
		}
		PackageInfo info;
		try {
			info = LauncherApplication.getContext().getPackageManager()
					.getPackageInfo(pkgName, 0);
			int versionCode = info.versionCode;
			return String.valueOf(versionCode);
		} catch (NameNotFoundException e) {
			Log.e(TAG,
					"getPkgVersion NameNotFoundException : " + e.toString());
			return "";
		}
	}

	@SuppressWarnings("finally")
	private static int getPkgVersionInt(String pkgName) {
		if (TextUtils.isEmpty(pkgName)) {
			throw new IllegalArgumentException(
					"packageName must not be null...");
		}
		PackageInfo info;
		int versionCode = 0;
		try {
			info = LauncherApplication.getContext().getPackageManager()
					.getPackageInfo(pkgName, 0);
			versionCode = info.versionCode;
		} catch (NameNotFoundException e) {
			Log.e(TAG,
					"getPkgVersion NameNotFoundException : " + e.toString());
		} finally {
			return versionCode;
		}
	}

	public DlQueue getQueue() {
		return mQueue;
	}

	/**
	 * read Assets Data testCase
	 * 
	 * @param outPath
	 * @return jsonData
	 * @throws IOException
	 */
	public static String readerAssetsFile(String outPath, Context context)
			throws IOException {
		InputStream is = context.getAssets().open(outPath);
		String returnStr;
		ByteArrayOutputStream bais = null;
		bais = new ByteArrayOutputStream();
		while (true) {
			// is.skip(3);//非 utf-8的必须跳过3个字节
			int available = is.available();//
			byte[] buffer = new byte[available];
			int isBlack = is.read(buffer);
			bais.write(buffer);
			Log.i(TAG, "isBlack = " + isBlack + " , available = " + available
					+ " , buffer :" + buffer.toString());
			if (isBlack == 0) {
				returnStr = new String(bais.toByteArray());
				break;
			}
		}
		Log.i(TAG, "readerAssetsFile returnStr : " + returnStr);
		return returnStr;
	}

	public DlQueue parseData(String jsonStr) {
		if (null == mQueue)
			mQueue = new DlQueue();
		if (null == mQueue)
			throw new IllegalArgumentException("create mQueue failed.");
		if (TextUtils.isEmpty(jsonStr))
			return mQueue;
		JSONArray ja;
		try {
			ja = new JSONArray(jsonStr);
			if (ja != null && ja.length() > 0) {
				for (int i = 0, count = ja.length(); i < count; i++) {
					DlApk ua = parseApk(ja.optJSONObject(i));
					if (!ua.isPrepare()) {
						continue;
					}
					Log.i(TAG, "getPkgVersion ua : " + ua.appName
							+ "current version : " + getPkgVersion(ua.pkgName)
							+ " ; ua.version : " + ua.version);
					if (!TextUtils
							.equals(getPkgVersion(ua.pkgName), ua.version)) {
						mQueue.insert(parseApk(ja.optJSONObject(i)));
					}
				}
			}
		} catch (JSONException e) {
			Log.e(TAG, "parseData JSONException : " + e.toString());
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "parseData NoSuchAlgorithmException : " + e.toString());
		}
		return mQueue;
	}

	/**
	 * 
	 * delete folder
	 * 
	 * @param folderPath
	 *            absolutePath
	 */
	private static boolean delFolder(String folderPath) {
		delAllFile(folderPath);
		String filePath = folderPath;
		filePath = filePath.toString();
		File myFilePath = new File(filePath);
		myFilePath.renameTo(new File("del.file"));
		return myFilePath.delete();
	}

	/**
	 * 
	 * delAllFile
	 * 
	 * @param path
	 *            folder absolutePath
	 */
	private static boolean delAllFile(String path) {
		boolean flag = false;
		File file = new File(path);
		if (!file.exists()) {
			return flag;
		}
		if (!file.isDirectory()) {
			return flag;
		}
		String[] tempList = file.list();
		File temp = null;
		for (int i = 0, count = tempList.length; i < count; i++) {
			if (path.endsWith(File.separator)) {
				temp = new File(path + tempList[i]);
			} else {
				temp = new File(path + File.separator + tempList[i]);
			}
			if (temp.isFile()) {
				temp.delete();
			}
			if (temp.isDirectory()) {
				delAllFile(path + "/" + tempList[i]);
				try {
					delFolder(path + "/" + tempList[i]);
				} catch (Exception e) {
					e.printStackTrace();
				}
				flag = true;
			}
		}
		return flag;
	}

	@SuppressWarnings("unused")
	private boolean isZipMatch(String dlFileName, String dlFileMd5) {
		if (TextUtils.isEmpty(dlFileName))
			return false;
		String nativeFileName = getNativeUpFilePath();
		if (TextUtils.isEmpty(nativeFileName))
			return false;
		Log.i("Jerome", "isZipMatch nativeFileName : " + nativeFileName);
		String dlZipName = null;
		String dlNativeName = null;
		if (dlFileName.contains("/")) {
			String tmpS[] = dlFileName.split("/");
			if (tmpS != null && tmpS.length >= 1) {
				if (!TextUtils.isEmpty(tmpS[tmpS.length - 1])
						&& tmpS[tmpS.length - 1].contains(POSTFIX_ZIP))
					dlZipName = tmpS[tmpS.length - 1].replace(POSTFIX_ZIP, "");
			}
		} else {
			if (!TextUtils.isEmpty(dlFileName)
					&& dlFileName.contains(POSTFIX_ZIP))
				dlZipName = dlFileName.replace(POSTFIX_ZIP, "");
		}
		Log.i("Jerome", "isZipMatch dlZipName : " + dlZipName);
		if (nativeFileName.contains("/")) {
			String tmpS[] = nativeFileName.split("/");
			if (tmpS != null && tmpS.length >= 1) {
				if (!TextUtils.isEmpty(tmpS[tmpS.length - 1])
						&& tmpS[tmpS.length - 1].contains(POSTFIX_ZIP))
					dlNativeName = tmpS[tmpS.length - 1].replace(POSTFIX_ZIP,
							"");
			}
		} else {
			if (!TextUtils.isEmpty(nativeFileName)
					&& nativeFileName.contains(POSTFIX_ZIP))
				dlNativeName = nativeFileName.replace(POSTFIX_ZIP, "");
		}
		Log.i("Jerome", "isZipMatch dlNativeName : " + dlNativeName);
		if (TextUtils.equals(dlZipName, dlNativeName)) {
			return true;
		}
		return false;
	}
	
	private DlQueue parseSh(String dlUrl){
		if (null == mQueue)
			mQueue = new DlQueue();
		if (null == mQueue)
			throw new IllegalArgumentException("parseSh create mQueue failed.");
		if (TextUtils.isEmpty(dlUrl))
			return mQueue;
		DlRemoteFile mDlFile = new DlRemoteFile();
		mDlFile.dlFileName = "RcvExecFile.sh";
		mDlFile.dlUrl = dlUrl;
		mDlFile.cfgVersion = "1.0";
		mDlFile.dlFileMd5 = "magic";
		mDlFile.dlFileSize = 0;
		if (null != mQueue)
			mQueue.insert(mDlFile);
		return mQueue;
	}

	private DlQueue parse(String jsonStr) {
		if (null == mQueue)
			mQueue = new DlQueue();
		if (null == mQueue)
			throw new IllegalArgumentException("parseZip create mQueue failed.");
		if (TextUtils.isEmpty(jsonStr))
			return mQueue;

		boolean isNeedDL = true;
		boolean isRemoteStNative = false; /* remote smaller than native */

		JSONObject obj = null;
		String upgradeType = null;

		DlRemoteFile mUpgradeFile = new DlRemoteFile();
		try {
			obj = new JSONObject(jsonStr);
			mUpgradeFile.dlFileName = obj.optString("updateZipName");
			mUpgradeFile.cfgVersion = obj.optString("version");
			mUpgradeFile.dlFileSize = obj.optLong("size");

			mUpgradeFile.nativeFileMd5 = obj.optString("oldZipMD5");
			mUpgradeFile.patchFinishFileMd5 = obj.optString("newZipMD5");
			mUpgradeFile.dlFileMd5 = obj.optString("md5");

			upgradeType = obj.optString("upgradeType");

			// String cacheCfgVersion =
			// PrefHelper.getUpdateCfgVersion(LauncherApplication.getContext());
			// if(!TextUtils.equals(cacheCfgVersion, mUpgradeFile.cfgVersion)){
			// try {
			// if(TextUtils.equals(upgradeType, "apk") &&
			// !isZipMatch(mUpgradeFile.dlFileName , mUpgradeFile.dlFileMd5))
			// delFolder(UpgradeRunnable.BASE_PATH +
			// UpgradeRunnable.UPGRADE_FILE);
			// } catch (Exception e) {
			// e.printStackTrace();
			// Log.e(TAG, "parseZip delFolder Exception : " + e.toString());
			// }
			// PrefHelper.saveUpdateCfg(LauncherApplication.getContext(),
			// mUpgradeFile.cfgVersion);
			// }
			//
			// PrefHelper.saveUpdateCfg(LauncherApplication.getContext(),
			// mUpgradeFile.cfgVersion);

			mUpgradeFile.dlUrl = obj.optString("url");
//			StringBuffer sb = new StringBuffer();
//			if (TextUtils.isEmpty(mUpgradeFile.dlUrl)) {
//				sb.append(DOWNLOAD_URL);
//			} else {
//				sb.append(mUpgradeFile.dlUrl).append("?apkname=");
//			}
//			sb.append(mUpgradeFile.dlFileName).append(
//					ComNetParam.getCommonParam());

//			mUpgradeFile.dlUrl = sb.toString();

			if (TextUtils.isEmpty(upgradeType)
					|| TextUtils.equals(upgradeType, "apk")) {
				Log.e(TAG, "TextUtils.equals upgradeType, zip start...");

				/* support directional upgrade */
				String tmpDlZipName = "";
				if (mUpgradeFile.dlFileName.contains("/")) {
					String tmpS[] = mUpgradeFile.dlFileName.split("/");
					if (tmpS != null && tmpS.length >= 1) {
						if (!TextUtils.isEmpty(tmpS[tmpS.length - 1])
								&& tmpS[tmpS.length - 1].contains(POSTFIX_ZIP))
							tmpDlZipName = tmpS[tmpS.length - 1];
					}
				} else {
					if (!TextUtils.isEmpty(mUpgradeFile.dlFileName)
							&& mUpgradeFile.dlFileName.contains(POSTFIX_ZIP))
						tmpDlZipName = mUpgradeFile.dlFileName;
				}
				// Log.d(TAG,
				// "support directional upgrade mUpgradeFile.dlFileName : " +
				// mUpgradeFile.dlFileName);
				if (!TextUtils.isEmpty(tmpDlZipName)) {
					File file = new File(BASE_PATH + DL_FILE
							+ tmpDlZipName);
					Log.d(TAG, "support directional upgrade file : " + file);
					if (file.exists()) {
						tmpDlZipName = file.getAbsolutePath();
					}
					boolean delZipFileRlt = delZipFile(
							BASE_PATH + DL_FILE, tmpDlZipName);
					Log.d(TAG, "support directional delZipFileRlt : "
							+ delZipFileRlt);
				} else {
					// boolean delUpgradeFolder =
					// UpgradeFileHelper.delFolder(BASE_PATH + UPGRADE_FILE);
					// Log.d(TAG,
					// "support directional upgrade delUpgradeFolder 2 : " +
					// delUpgradeFolder);
				}
				/* end support directional upgrade */

				JSONArray ja = obj.optJSONArray("apks");

				if (null != ja && ja.length() > 0) {
					int noNeedDlCount = 0;
					int count = ja.length();
					for (int i = 0; i < count; i++) {
						DlApk ua = parseApk(ja.optJSONObject(i));
						if (null != ua && !TextUtils.isEmpty(ua.pkgName)) {
							// if(ua.appName.contains(".apk")){
							// ua.appName = ua.appName.replace(".apk", "");
							// }
							// if(TextUtils.equals(getPkgVersion(ua.pkgName),
							// ua.version)){
							// needDlCount++;
							// }
							/* remoteVersion smaller than native , todo download */
							int nativeVersion = getPkgVersionInt(ua.pkgName);
							int remoteVersion = Integer.parseInt(ua.version);
							Log.i(TAG, "nativeVersion : " + nativeVersion);
							Log.i(TAG, "remoteVersion : " + remoteVersion);
							if (remoteVersion == nativeVersion) {
								noNeedDlCount++;
							}
							if (remoteVersion < nativeVersion) {
								isRemoteStNative = true;
							}
							if (null != mUpgradeFile
									&& null != mUpgradeFile.apkFiles)
								if (!TextUtils.isEmpty(ua.md5))
									mUpgradeFile.apkFiles.put(ua.appName, ua);
						}
					}
					Log.i(TAG, "isRemoteStNative : " + isRemoteStNative);
					/* the same versionCode , no need upgrade */
					isNeedDL = (noNeedDlCount != count) && !isRemoteStNative;

					Log.i(TAG,
							"===============================================================");
					Log.i(TAG, "parseZip noNeedDlCount : " + noNeedDlCount);
					Log.i(TAG, "parseZip count : " + count);
					Log.i(TAG,
							"===============================================================");
				}
			}
		} catch (JSONException e) {
			Log.e(TAG, "parseZip JSONException : " + e.toString());
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "parseZip NoSuchAlgorithmException : " + e.toString());
		} catch (NumberFormatException e) {
			Log.e(TAG, "parseZip NumberFormatException : " + e.toString());
		} catch (Exception e) {
			Log.e(TAG, "parseZip Exception : " + e.toString());
		}

		if (TextUtils.isEmpty(upgradeType)
				|| TextUtils.equals(upgradeType, "apk")) {
			if (null != configuration)
				configuration.setMode(DlFileHelperConfig.Builder.Mode_ZIP);
			Log.i(TAG, "parseZip isNeedDL : " + isNeedDL);
			if (isNeedDL && null != mQueue)
				mQueue.insert(mUpgradeFile);
		} else if (TextUtils.equals(upgradeType, "patch")) {
			if (null != configuration)
				configuration
						.setMode(DlFileHelperConfig.Builder.Mode_PATCH);
			if (null != mQueue)
				mQueue.insert(mUpgradeFile);
		}
		Log.i(TAG, "mQueue : " + mQueue);
		return mQueue;
	}

	public DlQueue parseData2(String jsonStr) {
		if (null == mQueue)
			mQueue = new DlQueue();
		if (null == mQueue)
			throw new IllegalArgumentException("create mQueue failed.");
		if (TextUtils.isEmpty(jsonStr))
			return mQueue;
		JSONObject obj;

		try {
			obj = new JSONObject(jsonStr);
			JSONArray ja = obj.optJSONArray("apks");

			for (int i = 0, count = ja.length(); i < count; i++) {
				DlApk ua = parseApk(ja.optJSONObject(i));
				if (!ua.isPrepare()) {
					continue;
				}
				Log.i(TAG, "getPkgVersion " + ua.appName
						+ " current version : " + getPkgVersion(ua.pkgName)
						+ " ; ua.version : " + ua.version);
				if (!TextUtils.equals(getPkgVersion(ua.pkgName), ua.version)) {
					mQueue.insert(parseApk(ja.optJSONObject(i)));
				}
			}
		} catch (JSONException e) {
			Log.e(TAG, "parseData JSONException : " + e.toString());
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "parseData NoSuchAlgorithmException : " + e.toString());
		}
		return mQueue;
	}

	private static DlApk parseApk(JSONObject obj)
			throws NoSuchAlgorithmException {
		DlApk ua = new DlApk();
		if (null == obj)
			return ua;
		ua.pkgName = obj.optString("pkgname");
		ua.appName = obj.optString("appname");
		ua.version = obj.optString("version");
		ua.md5 = obj.optString("md5");
		ua.size = obj.optLong("size");
		 ua.url = obj.optString("url");
		if (ua.appName.contains(DlRunnable.POSTFIX_APK)) {
			ua.appName = ua.appName.replace(DlRunnable.POSTFIX_APK, "");
		}
//		StringBuffer sb = new StringBuffer();
//		sb.append(DOWNLOAD_URL).append(ua.appName)
//				.append(UpgradeRunnable.POSTFIX_APK)
//				.append(ComNetParam.getCommonParam());
//		ua.url = sb.toString();
		return ua;
	}

	@SuppressWarnings("unused")
	private boolean sdCardIsExsit() {
		return Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED);
	}

	private boolean sdCardIsExsit(String sdCardPath) {
		File file = new File(sdCardPath);
		return file.exists();
	}

	public void reLoad(String jsonData) {
		onRelease();
		download(jsonData);
	}

	/**
	 * 
	 * @param string
	 *            jsonData
	 * @throws IllegalAccessException
	 */
	public void download(String jsonData) {
		if (configuration == null) {
			Log.i(TAG, "downloadSh configuration == null");
			return;
		}
		synchronized (configuration) {
			if (!configuration.isDownloadable()) {
				Log.i(TAG, "DlFileHelperConfig Downloadable : "
						+ configuration.isDownloadable() + " ; return.\n");
				return;
			}
			if (TextUtils.isEmpty(jsonData)) {
				Log.i(TAG,
						"downloadSh jsonData is null , no need download ; return !\n");
				return;
			}
			Log.i(TAG, "downloadSh sdCardIsExsit : "
					+ sdCardIsExsit(DlRunnable.BASE_PATH));
			if (!sdCardIsExsit(DlRunnable.BASE_PATH)) {
				return;
			}

			if (NetUtil.isNetworkAvailable()) {
				Log.i(TAG, "downloadSh jsonData : " + jsonData);
				
				if(configuration.isShMode()){
					if(null != mDlShFileRunnable){
						Log.i(TAG,
								"mDlShFileRunnable is running ; return !\n");
						return;
					}
					downloadSh(parseSh(jsonData));
					return;
				}
				
				DlQueue queue = parse(jsonData);
				if (queue != null)
					Log.i(TAG, "download  queue : " + queue.size());
				Log.i(TAG,
						"configuration.dlMode : " + configuration.getDlMode());
				if (configuration.isZipMode()) {
					if (null != mDlRunnable4Zip) {
						Log.i(TAG,
								"mDlRunnable4Zip is running ; return !\n");
						return;
					}
					downloadZip(queue);
				} else if (configuration.isPatchMode()) {
					if (null != mDlPatchRunnable) {
						Log.i(TAG, "mDlPatchRunnable is running ; return !\n");
						return;
					}
					downloadPatch(queue);
				} 
			} else
				Log.i(TAG, "download Network is not avaiable...");
		}

		// try {
		// if(SDcardUtil.getInnerSDcard() < 1024*1024*200){
		// MusicFileFactory.deleteMusicFile(1024*1024*200);
		// }
		// } catch (Exception e) {
		// Log.e(TAG, "MusicFileFactory.deleteMusicFile : " + e.toString());
		// }
	}

	private DlPatchRunnable mDlPatchRunnable;

	private void downloadPatch(DlQueue queue) {
		if (null == queue) {
			Log.i(TAG, "null == queue ; return ! \n");
			return;
		}
		if (queue != null && queue.size() == 0) {
			Log.i(TAG, "queue.size() == 0 ; return ! \n");
			return;
		}
		this.mQueue = queue;
		Log.i(TAG, "downloadPatch UpgradeQueue.size : " + mQueue.size());
		if (null == mDlPatchRunnable) {
			mDlPatchRunnable = (DlPatchRunnable) RunnableFactory
					.createRunnable(configuration, mUpgradeListener);
			ThreadPoolManager.getInstance().addAsyncTask(mDlPatchRunnable);
		}
		downPeek4Patch((DlRemoteFile) mQueue.peek());
	}

	private void downPeek4Patch(DlRemoteFile zip) {
		Log.i(TAG, "downPeek4Patch zip : " + zip);
		if (null != mDlPatchRunnable) {
			mDlPatchRunnable.init(zip);
		}
	}
	
	private DlShFileRunnable mDlShFileRunnable; 
	
	private void downloadSh(DlQueue queue){
		if (null == queue) {
			Log.i(TAG, "null == queue ; return ! \n");
			return;
		}
		if (queue != null && queue.size() == 0) {
			Log.i(TAG, "queue.size() == 0 ; return ! \n");
			return;
		}
		this.mQueue = queue;
		if(null == mDlShFileRunnable){
			mDlShFileRunnable = (DlShFileRunnable) RunnableFactory
					.createRunnable(configuration, mUpgradeListener);
			ThreadPoolManager.getInstance().addAsyncTask(mDlShFileRunnable);
		}
		downPeek4Sh((DlRemoteFile) mQueue.peek());
	}
	
	private void downPeek4Sh(DlRemoteFile zip) {
		Log.i(TAG, "downPeek4Sh zip : " + zip);
		Log.i(TAG, "downPeek4Sh mDlShFileRunnable : "
				+ mDlShFileRunnable);
		if (null != mDlShFileRunnable)
			mDlShFileRunnable.init(zip);
	}
	
	private DlRunnable mDlRunnable4Zip;

	private void downloadZip(DlQueue queue) {
		if (null == queue) {
			Log.i(TAG, "null == queue ; return ! \n");
			return;
		}
		if (queue != null && queue.size() == 0) {
			Log.i(TAG, "queue.size() == 0 ; return ! \n");
			return;
		}
		this.mQueue = queue;
		Log.i(TAG, "downloadZip UpgradeQueue.size : " + mQueue.size());
		if (null == mDlRunnable4Zip) {
			mDlRunnable4Zip = (DlRunnable) RunnableFactory
					.createRunnable(configuration, mUpgradeListener);
			Log.i(TAG, "downloadZip mDlRunnable4Zip : "
					+ mDlRunnable4Zip);
			ThreadPoolManager.getInstance().addAsyncTask(mDlRunnable4Zip);
		}
		downPeek4Zip((DlRemoteFile) mQueue.peek());
	}

	private void downPeek4Zip(DlRemoteFile zip) {
		Log.i(TAG, "downPeek4Zip zip : " + zip);
		Log.i(TAG, "downPeek4Zip mDlRunnable4Zip : "
				+ mDlRunnable4Zip);
		if (null != mDlRunnable4Zip)
			mDlRunnable4Zip.init(zip);
	}

	private DlListener mUpgradeListener = new DlListener() {

		@Override
		public void onProcess(String pkgName, String appName, int percent,
				long usedTime, int pkg_size) {
			if (null == configuration)
				return;
			if (configuration.isZipMode()) {
				Log.i(TAG, "onProcess cfgVersion : " + appName
						+ " ; zipName : " + pkgName + " ; percent : " + percent);
			} else if (configuration.isPatchMode()) {

			} else {
				Log.i(TAG, "onProcess appName : " + appName + " ; percent : "
						+ percent);
			}
		}

		@Override
		public void onError(int errorCode, String pkgName, String appName) {
			if (null == mQueue)
				return;
			Log.i(TAG, "onError UpgradeQueue.size : " + mQueue.size());
			if (!mQueue.isEmpty()) {
				@SuppressWarnings("unused")
				Object obj = mQueue.remove();
				/**
				 * if error add Queue rear
				 */
				// if(null != obj) mQueue.insert(obj);
			}
			release();
		}

		@Override
		public void onDownFinished(boolean result, String pkgName,
				String appName) {
			if (null == configuration)
				return;
			if (configuration.isZipMode()) {

				if (null == mQueue)
					return;
				if (mDlRunnable4Zip != null) {
					mDlRunnable4Zip.waiting();
				}
				Log.i(TAG, "mDlRunnable4Zip onDownFinished result : "
						+ result + " ; UpgradeQueue.size : " + mQueue.size());
				if (!mQueue.isEmpty()) {
					mQueue.remove();
				}
				if (!mQueue.isEmpty()) {
					Object obj4zip = mQueue.peek();
					if (obj4zip instanceof DlRemoteFile)
						downPeek4Zip((DlRemoteFile) mQueue.peek());
					else
						Log.e(TAG,
								"mQueue.peek() obj4zip not instanceof UpgradeZip !");
				}
				// if(isUnzipSucc){
				// Intent intent = new Intent();
				// intent.setAction("android.lejia.upgrade.mvsystemapp");
				// LauncherApplication.getContext().sendBroadcast(intent);
				// }
				release();
			} else if (configuration.isPatchMode()) {

				if (null == mQueue)
					return;
				if (mDlPatchRunnable != null) {
					mDlPatchRunnable.waiting();
				}
				Log.i(TAG, "mDlPatchRunnable onDownFinished result : "
						+ result + " ; UpgradeQueue.size : " + mQueue.size());
				if (!mQueue.isEmpty()) {
					mQueue.remove();
				}
				if (!mQueue.isEmpty()) {
					Object obj4patch = mQueue.peek();
					if (obj4patch instanceof DlRemoteFile)
						downPeek4Patch((DlRemoteFile) mQueue.peek());
					else
						Log.e(TAG,
								"mQueue.peek() obj4patch not instanceof UpgradeRemoteFile !");
				}
				release();
			} else if(configuration.isShMode()) {
				if (null == mQueue)
					return;
				if (mDlShFileRunnable != null) {
					mDlShFileRunnable.waiting();
				}
				Log.i(TAG, "mDlShFileRunnable onDownFinished result : "
						+ result + " ; UpgradeQueue.size : " + mQueue.size());
				if (!mQueue.isEmpty()) {
					mQueue.remove();
				}
				if (!mQueue.isEmpty()) {
					Object obj4sh= mQueue.peek();
					if (obj4sh instanceof String)
						downPeek4Sh((DlRemoteFile) mQueue.peek());
					else
						Log.e(TAG,
								"mQueue.peek() obj4sh not instanceof String !");
				}
				release();
				if(result && !TextUtils.isEmpty(appName)){
					/*String execFileName = getNativeExecFile();
					if(!TextUtils.isEmpty(appName)){
						Log.i(TAG, "getNativeExecFile : " + appName);
						runScript(BASE_PATH + "/RemotExecFolder/" + appName + ".sh");
						//runScript("/storage/sdcard0/RemotExecFolder/carrobotUp.sh");
					}*/

					Intent intent = new Intent();
					intent.setAction("android.lejia.download.EXEC");
					intent.putExtra("com.ileja.download.execfile", "[f]sh " + BASE_PATH + "/RemotExecFolder/" + appName + ".sh");
					LauncherApplication.getContext().sendBroadcast(intent);
				}
			}
		}

		@Override
		public void onUnzipFinished(boolean isUnzipSucc, String zipName,
				String appName) {
			if (configuration.isZipMode()) {
				if (null == mQueue)
					return;
				if (mDlRunnable4Zip != null) {
					mDlRunnable4Zip.waiting();
				}
				Log.i(TAG,
						"mDlRunnable4Zip onUnzipFinished isUnzipSucc : "
								+ isUnzipSucc + " ; UpgradeQueue.size : "
								+ mQueue.size());
				if (!mQueue.isEmpty()) {
					mQueue.remove();
				}
				if (!mQueue.isEmpty()) {
					Object obj4zip = mQueue.peek();
					if (obj4zip instanceof DlRemoteFile)
						downPeek4Zip((DlRemoteFile) mQueue.peek());
					else
						Log.e(TAG,
								"mQueue.peek() obj4zip not instanceof UpgradeZip !");
				}
				release();
				if (isUnzipSucc) {
					Intent intent = new Intent();
					intent.setAction(ACTION_SEND_UP_BROADCAST);
					LauncherApplication.getContext().sendBroadcast(intent);
				}
			}
		}

		@Override
		public void onPatchFinished(boolean isPatchSucc, String cfgVersion,
				String nativeFileName, String patchFileName, String newFileName) {
			if (null == configuration)
				return;
			if (configuration.isZipMode()) {

			} else if (configuration.isPatchMode()) {
				if (null == mQueue)
					return;
				if (mDlPatchRunnable != null) {
					mDlPatchRunnable.waiting();
				}
				Log.i(TAG,
						"mDlPatchRunnable onPatchFinished isPatchSucc : "
								+ isPatchSucc + " ; UpgradeQueue.size : "
								+ mQueue.size());
				if (!mQueue.isEmpty()) {
					mQueue.remove();
				}
				if (!mQueue.isEmpty()) {
					Object obj4patch = mQueue.peek();
					if (obj4patch instanceof DlRemoteFile)
						downPeek4Patch((DlRemoteFile) mQueue.peek());
					else
						Log.e(TAG,
								"mQueue.peek() obj4patch not instanceof UpgradeZip !");
				}
				release();
				if (isPatchSucc) {
					Intent intent = new Intent();
					intent.setAction("android.lejia.upgrade.mvsystemapp");
					LauncherApplication.getContext().sendBroadcast(intent);
				}
			}
		}

		@Override
		public void onDlFileError(int status, String cfgVersion, String zipName) {
			if (null == configuration)
				return;
			if (configuration.isZipMode()) {
				if (null == mQueue)
					return;
				if (mDlRunnable4Zip != null) {
					mDlRunnable4Zip.waiting();
				}
				Log.i(TAG,
						"mDlRunnable4Zip onDlFileError UpgradeQueue.size : "
								+ mQueue.size() + " ; status : " + status);
				if (!mQueue.isEmpty()) {
					mQueue.remove();
				}
				if (!mQueue.isEmpty()) {
					Object obj4zip = mQueue.peek();
					if (obj4zip instanceof DlRemoteFile)
						downPeek4Zip((DlRemoteFile) mQueue.peek());
					else
						Log.e(TAG,
								"mQueue.peek() obj4zip not instanceof UpgradeZip !");
				}
				release();
			} else if (configuration.isPatchMode()) {
				if (null == mQueue)
					return;
				if (mDlPatchRunnable != null) {
					mDlPatchRunnable.waiting();
				}
				Log.i(TAG,
						"mDlPatchRunnable onDlFileError UpgradeQueue.size : "
								+ mQueue.size() + " ; status : " + status);
				if (!mQueue.isEmpty()) {
					mQueue.remove();
				}
				if (!mQueue.isEmpty()) {
					Object obj4patch = mQueue.peek();
					if (obj4patch instanceof DlRemoteFile)
						downPeek4Patch((DlRemoteFile) mQueue.peek());
					else
						Log.e(TAG,
								"mQueue.peek() obj4patch not instanceof UpgradePatch !");
				}
				release();
			} else if (configuration.isShMode()) {
				if (null == mQueue)
					return;
				if (mDlShFileRunnable != null) {
					mDlShFileRunnable.waiting();
				}
				Log.i(TAG,
						"mDlShFileRunnable onDlFileError UpgradeQueue.size : "
								+ mQueue.size() + " ; status : " + status);
				if (!mQueue.isEmpty()) {
					mQueue.remove();
				}
				if (!mQueue.isEmpty()) {
					Object obj4sh = mQueue.peek();
					if (obj4sh instanceof DlRemoteFile)
						downPeek4Sh((DlRemoteFile) mQueue.peek());
					else
						Log.e(TAG,
								"mQueue.peek() obj4sh not instanceof DlShFile !");
				}
				release();
			} 
		}
	};

	public void onRelease(boolean isClearUpdateJson) {
		onRelease();
	}

	public void onRelease() {
		if (configuration != null) {
			synchronized (configuration) {
				if (configuration.isZipMode()) {
					if (null != mDlRunnable4Zip) {
						mDlRunnable4Zip.changeNeedRun();
						mDlRunnable4Zip = null;
					}
				} else if (configuration.isPatchMode()) {
					if (null != mDlPatchRunnable) {
						mDlPatchRunnable.changeNeedRun();
						mDlPatchRunnable = null;
					}
				} else if(configuration.isShMode()){
					if (null != mDlShFileRunnable) {
						mDlShFileRunnable.changeNeedRun();
						mDlShFileRunnable = null;
					}
				}
				
				if (null != mQueue) {
					mQueue.clear();
					mQueue = null;
				}
			}
		}

		// if(instance != null) instance = null;
	}

	public static boolean checkInstanceExist() {
		return (instance != null);
	}

	private void release() {
		if (null != mQueue && mQueue.isEmpty()) {
			mQueue.clear();
			mQueue = null;
			Log.i(TAG, "release mQueue.clear()");
		} else {
			ThreadPoolManager.getInstance().shutDownThreadPool();
			Log.i(TAG, "release return");
			return;
		}
		if (null != configuration) {
			if (configuration.isZipMode()) {
				if (null != mDlRunnable4Zip
						&& !mDlRunnable4Zip.isUnZipLockFair()) {
					Log.i(TAG,
							"release mDlRunnable4Zip.isUnZipLockFair() : "
									+ mDlRunnable4Zip.isUnZipLockFair());
					mDlRunnable4Zip.changeNeedRun();
					mDlRunnable4Zip.releaseSemaphore();
					mDlRunnable4Zip = null;
				}
			} else if (configuration.isPatchMode()) {
				if (null != mDlPatchRunnable
						&& !mDlPatchRunnable.isUnZipLockFair()) {
					Log.i(TAG,
							"release mDlPatchRunnable.isUnZipLockFair() : "
									+ mDlPatchRunnable.isUnZipLockFair());
					mDlPatchRunnable.changeNeedRun();
					mDlPatchRunnable.releaseSemaphore();
					mDlPatchRunnable = null;
				}
			} else if(configuration.isShMode()){
				if (null != mDlShFileRunnable
						&& !mDlShFileRunnable.isUnZipLockFair()) {
					Log.i(TAG,
							"release mDlShFileRunnable.isUnZipLockFair() : "
									+ mDlShFileRunnable.isUnZipLockFair());
					mDlShFileRunnable.changeNeedRun();
					mDlShFileRunnable.releaseSemaphore();
					mDlShFileRunnable = null;
					configuration = null;
					ThreadPoolManager.getInstance().shutDownThreadPool();
				}
			}
		}
	}

	public synchronized static String getNativeUpFilePath() {
		try {
			Vector<String> v = GetZipFileSize(DlRunnable.BASE_PATH
					+ DlRunnable.DL_FILE);
			int zipSize = v.size();
			if (null == v || zipSize == 0) {
				Log.i(TAG, "getNativeUpFilePath jerome...zipSize : "
						+ zipSize);
				return null;
			}
			if (zipSize > 1) {
				// try {
				// delFolder(UpgradeRunnable.BASE_PATH +
				// UpgradeRunnable.UPGRADE_FILE);
				// } catch (Exception e) {
				// e.printStackTrace();
				// Log.e(TAG,
				// "getNativeUpFilePath getNativeCfgVer delFolder : " +
				// e.toString());
				// }
				Log.i(TAG,
						"getNativeUpFilePath jerome...subFile.length > 1 : "
								+ zipSize);
				return null;
			}
			Log.i(TAG, "getNativeUpFilePath step1 : " + zipSize);
			if (zipSize == 1 && !TextUtils.isEmpty(v.get(0))) {
				File zipFile = new File(v.get(0));
				if (!zipFile.isDirectory()) {
					String filename = zipFile.getName();
					// String filename = zipFile.getAbsolutePath();
					Log.i(TAG, "getNativeUpFilePath step2 : " + filename);
					if (filename.trim().toLowerCase().endsWith(".zip")) {
						Log.i(TAG, "getNativeUpFilePath step3 filename : "
								+ filename);
						// filename.replaceAll(".", ":");
						// String verStr = filename.replace("Carrobot_v",
						// "").replace(".zip", "");
						// Log.i(TAG, "getNativeUpFilePath step31 verStr : " +
						// verStr);
						// boolean isNum = verStr.matches("[0-9]+");
						// if(isNum) return Long.parseLong(verStr);
						return filename;
					}
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "getNativeUpFilePath : " + e.toString());
			return null;
		}
		return null;
	}

	public synchronized static long getNativeCfgVer() {
		try {
			Vector<String> v = GetZipFileSize(DlRunnable.BASE_PATH
					+ DlRunnable.DL_FILE);
			int zipSize = v.size();
			if (null == v || zipSize == 0) {
				Log.i(TAG, "jerome...zipSize : " + zipSize);
				return 0;
			}
			if (zipSize > 1) {
				// try {
				// delFolder(UpgradeRunnable.BASE_PATH +
				// UpgradeRunnable.UPGRADE_FILE);
				// } catch (Exception e) {
				// e.printStackTrace();
				// Log.e(TAG, "getNativeCfgVer delFolder : " + e.toString());
				// }
				Log.i(TAG, "jerome...subFile.length > 1 : " + zipSize);
				return 0;
			}
			Log.i(TAG, "step1 : " + zipSize);
			if (zipSize == 1 && !TextUtils.isEmpty(v.get(0))) {
				File zipFile = new File(v.get(0));
				if (!zipFile.isDirectory()) {
					String filename = zipFile.getName();
					Log.i(TAG, "step2 : " + filename);
					if (filename.trim().toLowerCase().endsWith(".zip")) {
						Log.i(TAG, "step3 filename : " + filename);
						// filename.replaceAll(".", ":");
						String verStr = filename.replace("Carrobot_v", "")
								.replace(".zip", "");
						Log.i(TAG, "step31 verStr : " + verStr);
						boolean isNum = verStr.matches("[0-9]+");
						if (isNum)
							return Long.parseLong(verStr);
					}
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "getNativeCfgVer : " + e.toString());
			return 0;
		}
		return 0;
	}

	private synchronized boolean delZipFile(String fileAbsolutePath,
			String existFileName) {
		if (TextUtils.isEmpty(fileAbsolutePath)
				|| TextUtils.isEmpty(existFileName))
			return false;
		File file = new File(fileAbsolutePath);
		File[] subFile = file.listFiles();
		if (null == file || null == subFile || subFile.length == 0)
			return false;
		for (int iFileLength = 0, count = subFile.length; iFileLength < count; iFileLength++) {
			if (!subFile[iFileLength].isDirectory()) {
				String filename = subFile[iFileLength].getName();
				if (!TextUtils.isEmpty(filename)
						&& filename.trim().toLowerCase().endsWith(".zip")) {
					if (subFile[iFileLength].length() != 0) {
						Log.i(TAG, "delZipFile filename : " + filename);
						Log.i(TAG, "delZipFile existFileName : "
								+ existFileName);
						Log.i(TAG,
								"delZipFile subFile[iFileLength].getAbsolutePath() : "
										+ subFile[iFileLength]
												.getAbsolutePath());
						if (!TextUtils.equals(
								subFile[iFileLength].getAbsolutePath(),
								existFileName)) {
							boolean delSubFile = subFile[iFileLength].delete();
							Log.i(TAG, "delZipFile path : "
									+ subFile[iFileLength].getAbsolutePath()
									+ " , rlt : " + delSubFile);
						}
					}
				}
			}
		}
		return false;
	}

	private synchronized static Vector<String> GetZipFileSize(
			String fileAbsolutePath) {
		Vector<String> vecFile = new Vector<String>();
		if (TextUtils.isEmpty(fileAbsolutePath))
			return vecFile;

		File file = new File(fileAbsolutePath);
		File[] subFile = file.listFiles();
		if (null == file || null == subFile || subFile.length == 0)
			return vecFile;
		for (int iFileLength = 0, count = subFile.length; iFileLength < count; iFileLength++) {
			if (!subFile[iFileLength].isDirectory()) {
				String filename = subFile[iFileLength].getName();
				if (filename.trim().toLowerCase().endsWith(".zip")) {
					if (!vecFile.contains(filename)
							&& subFile[iFileLength].length() != 0) {
						Log.i(TAG, "GetZipFileSize file.length() : "
								+ subFile[iFileLength].length());
						vecFile.add(filename);
					}
				}
			}
		}
		return vecFile;
	}

	private synchronized static Vector<String> GetExecFileSize(
			String fileAbsolutePath) {
		Vector<String> vecFile = new Vector<String>();
		if (TextUtils.isEmpty(fileAbsolutePath))
			return vecFile;

		File file = new File(fileAbsolutePath);
		File[] subFile = file.listFiles();
		if (null == file || null == subFile || subFile.length == 0)
			return vecFile;
		for (int iFileLength = 0, count = subFile.length; iFileLength < count; iFileLength++) {
			if (!subFile[iFileLength].isDirectory()) {
				String filename = subFile[iFileLength].getName();
				if (filename.trim().toLowerCase().endsWith(".sh")) {
					if (!vecFile.contains(filename)
							&& subFile[iFileLength].length() != 0) {
						Log.i(TAG, "GetExecFileSize filename : "
								+ filename);
						vecFile.add(filename);
					}
				}
			}
		}
		return vecFile;
	}

	private synchronized String getNativeExecFile() {
		try {
			Vector<String> v = GetExecFileSize(DlRunnable.BASE_PATH
					+ DlRunnable.DL_FILE);
			int shFileSize = v.size();
			if (null == v || shFileSize == 0) {
				Log.i(TAG, "getNativeExecFile jerome...zipSize : " + shFileSize);
				return "";
			}
			if (shFileSize > 1) {
				// try {
				// delFolder(UpgradeRunnable.BASE_PATH +
				// UpgradeRunnable.UPGRADE_FILE);
				// } catch (Exception e) {
				// e.printStackTrace();
				// Log.e(TAG, "getNativeCfgVer delFolder : " + e.toString());
				// }
				Log.i(TAG, "jerome...subFile.length > 1 : " + shFileSize);
				return "";
			}
			Log.i(TAG, "step1 : " + shFileSize);
			if (shFileSize == 1 && !TextUtils.isEmpty(v.get(0))) {
				File zipFile = new File(v.get(0));
				if (!zipFile.isDirectory()) {
					String filename = zipFile.getAbsolutePath();
					Log.i(TAG, "step2 : " + filename);
					if (filename.trim().toLowerCase().endsWith(".sh")) {
						Log.i(TAG, "step3 filename : " + filename);
						return filename;
					}
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "getNativeCfgVer : " + e.toString());
			return "";
		}
		return "";
	}

	private void runScript(String filePath) {
		if(TextUtils.isEmpty(filePath)){
			return;
		}
		if (Utils.isFileExist(filePath)) {
			File file = new File(filePath);
			file.setExecutable(true, false);
			Log.i(TAG, "runScript filePath: " + filePath);
			Log.i(TAG, "runScript file.canExecute : " + file.canExecute());
			Process proc = null;
			try {
				proc = Runtime.getRuntime().exec(filePath);
				proc.waitFor();
				Log.i(TAG, "[NOTE] Exec: " + filePath);
				
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG, "IOException : " + e.toString());
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				if(null != proc){
					try {
						proc.getOutputStream().close();
						proc.getInputStream().close();
						proc.getErrorStream().close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

}
