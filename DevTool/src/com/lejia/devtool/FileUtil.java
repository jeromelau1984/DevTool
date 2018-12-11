package com.lejia.devtool;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtil {

	private static final String TAG = "FileUtil";

	static final int BUFF_SIZE = 1024 * 512;
	private static ExecutorService mSingleThreadExecutor = Executors
			.newSingleThreadExecutor();

	public static String getCacheAbsolutePath(Context context) {
		// Check if media is mounted or storage is built-in, if so, try and use
		// external cache dir
		// otherwise use internal cache dir
		boolean shouldUseExternalCache = Environment.MEDIA_MOUNTED
				.equals(Environment.getExternalStorageState())
				|| !isExternalStorageRemovable();

		File fileCacheDir = shouldUseExternalCache ? getExternalCacheDir(context)
				: context.getCacheDir();

		if (fileCacheDir == null) {
			fileCacheDir = context.getCacheDir();
		}

		final String cachePath = fileCacheDir.getPath();
		return cachePath + File.separator;
	}

	/**
	 * Check if external storage is built-in or removable.
	 * 
	 * @return True if external storage is removable (like an SD card), false
	 *         otherwise.
	 */
	@TargetApi(9)
	public static boolean isExternalStorageRemovable() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			return Environment.isExternalStorageRemovable();
		}
		return true;
	}

	/**
	 * Get the external app cache directory.
	 * 
	 * @param context
	 *            The context to use
	 * @return The external cache dir
	 */
	@TargetApi(8)
	public static File getExternalCacheDir(Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			if (Environment.MEDIA_MOUNTED.equals(Environment
					.getExternalStorageState())) {
				return context.getExternalCacheDir();
			}
		}
		return null;
	}

	/**
	 * 将文本写入到文件中
	 * 
	 * @param fileName
	 * @param content
	 */
	public static void saveStringToFile(String fileName, String content) {
		File file = new File(fileName);
		FileWriter fw = null;
		try {
			File parent = file.getParentFile();
			if (parent != null && !parent.exists()) {
				parent.mkdirs();
			}
			// fixed HuaWei C8813D file not found exception
			if (parent == null || !parent.exists()) {
				return;
			}
			fw = new FileWriter(file);
			fw.write(content);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fw != null) {
					fw.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 读取文本文件内容
	 * 
	 * @param filename
	 * @return
	 */
	public static String readFileContent(String filename) {
		File file = new File(filename);
		if (!file.exists() || !file.isFile()) {
			return null;
		}

		FileReader fr = null;
		BufferedReader br = null;
		StringBuilder content = new StringBuilder();
		try {
			fr = new FileReader(file);
			br = new BufferedReader(fr);
			String line = null;
			while ((line = br.readLine()) != null) {
				content.append(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (fr != null) {
				try {
					fr.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return content.toString();
	}

	/**
	 * 将文本写入到文件中
	 * 
	 * @param filePath
	 * @param content
	 * @param isAppend
	 *            是否追加
	 */
	public static void saveStringToFile(final String filePath,
			final String content, final boolean isAppend) {
		mSingleThreadExecutor.submit(new Runnable() {

			@Override
			public synchronized void run() {
				if (TextUtils.isEmpty(filePath)
						|| filePath.matches("null\\/.*")) {
					Log.w(TAG, "The file to be saved is null!");
					return;
				}
				FileWriter fw = null;
				try {
					File file = new File(filePath);
					File parent = file.getParentFile();
					if (parent != null && !parent.exists()) {
						parent.mkdirs();
					}

					fw = new FileWriter(file, isAppend);
					fw.write(content);
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						if (fw != null) {
							fw.close();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	public static void deleteAllFiles(File root) {
		File files[] = root.listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) { // 判断是否为文件夹
					deleteAllFiles(f);
					try {
						f.delete();
					} catch (Exception e) {
					}
				} else {
					if (f.exists()) { // 判断是否存在
						try {
							f.delete();
						} catch (Exception e) {
						}
					}
				}
			}
		}
	}

	public static void delFolder(String folderPath) throws Exception {
		delAllFile(folderPath);
		String filePath = folderPath;
		filePath = filePath.toString();
		File myFilePath = new File(filePath);
		myFilePath.delete();
	}

	public static boolean delAllFile(String path) {
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
		for (int i = 0; i < tempList.length; i++) {
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
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				flag = true;
			}
		}
		return flag;
	}

	public static String format(String s) {
		String str = s.replaceAll("。.", ".").replaceAll("[!！？~]", "")
				.replaceAll("(…{2})", "").replaceAll("(\\.{6})", "");
		return str;
	}

	public static synchronized int copyFile(Context context, String fileName,
			String absFilePath) {
		if (context == null || TextUtils.isEmpty(fileName)
				|| TextUtils.isEmpty(absFilePath)) {
			return -1;
		}
		Log.i(TAG, "copyFile : " + fileName);
		Log.i(TAG, "absFilePath : " + absFilePath);
		InputStream fis = null;
		FileOutputStream fos = null;
		try {
			File file = new File(absFilePath);
			fis = new FileInputStream(file);
			fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
			byte[] data = new byte[BUFF_SIZE];
			int len = 0;

			while ((len = fis.read(data)) != -1) {
				fos.write(data, 0, len);
			}
			fos.close();
			fis.close();
			return 1;
		} catch (FileNotFoundException e) {
			Log.e(TAG, "copyFile FileNotFoundException : " + e.toString());
		} catch (IOException e) {
			Log.e(TAG, "copyFile IOException : " + e.toString());
		} finally {
			try {
				fis.close();
				fos.close();
			} catch (IOException e) {
				Log.e(TAG, "copyFile finally IOException : " + e.toString());
			}
		}
		return -1;
	}

	/**
	 * does not close io
	 * 
	 * @param is
	 * @param file
	 * @return true MD5 matches
	 */
	public static boolean checkMD5(final InputStream is, File file) {
		if (file.exists()) {
			try {
				FileInputStream destFis = new FileInputStream(file);
				byte[] md5_1 = getFileMD5String(destFis);
				byte[] md5_2 = getFileMD5String(is);
				boolean same = true;
				int minLength = (md5_1.length > md5_2.length) ? md5_2.length
						: md5_1.length;
				for (int k = 0; k < minLength; k++) {
					if (md5_1[k] != md5_2[k]) {
						same = false;
						break;
					}
				}
				destFis.close();
				return same;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	private static byte[] getFileMD5String(InputStream in) {
		try {
			MessageDigest messagedigest = MessageDigest.getInstance("MD5");

			byte[] buffer = new byte[BUFF_SIZE];
			int length = -1;
			while ((length = in.read(buffer)) != -1) {
				messagedigest.update(buffer, 0, length);
			}
			return messagedigest.digest();

		} catch (NoSuchAlgorithmException nsaex) {
			nsaex.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new byte[0];
	}

	public static byte[] MAGIC = { 'P', 'K', 0x3, 0x4 };

	/**
	 * check zip
	 * 
	 * @param f
	 * @return
	 */
	public static boolean isZipFile(File f) {
		boolean isZip = true;
		byte[] buffer = new byte[MAGIC.length];
		try {
			RandomAccessFile raf = new RandomAccessFile(f, "r");
			raf.readFully(buffer);
			for (int i = 0; i < MAGIC.length; i++) {
				if (buffer[i] != MAGIC[i]) {
					isZip = false;
					break;
				}
			}
			raf.close();
		} catch (Throwable e) {
			isZip = false;
		}
		return isZip;
	}

	/**
	 * unzip zipfile under destDir, support subdir
	 * 
	 * @param zipfileName
	 *            the zip file
	 * @param destDir
	 *            destination dir
	 */
	public static void unZip(final Context context, File zipfileName) {
		byte data[] = new byte[BUFF_SIZE];
		ZipFile zipFile;
		try {
			zipFile = new ZipFile((zipfileName));
			Enumeration<? extends ZipEntry> emu = zipFile.entries();
			while (emu.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) emu.nextElement();
				if (entry.isDirectory()) {
					new File(context.getFilesDir().getAbsolutePath(),
							entry.getName()).mkdirs();

					continue;
				}
				BufferedInputStream bis = new BufferedInputStream(
						zipFile.getInputStream(entry));
				Log.d(TAG, format(entry.getName()));
				OutputStream outputStream = new FileOutputStream(new File(
						getResourceDir(context), format(entry.getName())));
				BufferedOutputStream bos = new BufferedOutputStream(
						outputStream, BUFF_SIZE);
				int readSize;
				while ((readSize = bis.read(data, 0, BUFF_SIZE)) != -1) {
					bos.write(data, 0, readSize);
				}
				bos.flush();
				bos.close();
				bis.close();
			}
			zipFile.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static String getResourceDir(Context context) {
		if (context == null) {
			return null;
		}
		return context.getFilesDir().getAbsolutePath();
	}


	public static boolean isTopURL(String str){
    
      str = str.toLowerCase();
      String domainRules = "com.cn|net.cn|org.cn|gov.cn|com.hk|com|net|org|int|edu|gov|mil|arpa|Asia|biz|info|name|pro|coop|aero|museum|ac|ad|ae|af|ag|ai|al|am|an|ao|aq|ar|as|at|au|aw|az|ba|bb|bd|be|bf|bg|bh|bi|bj|bm|bn|bo|br|bs|bt|bv|bw|by|bz|ca|cc|cf|cg|ch|ci|ck|cl|cm|cn|co|cq|cr|cu|cv|cx|cy|cz|de|dj|dk|dm|do|dz|ec|ee|eg|eh|es|et|ev|fi|fj|fk|fm|fo|fr|ga|gb|gd|ge|gf|gh|gi|gl|gm|gn|gp|gr|gt|gu|gw|gy|hk|hm|hn|hr|ht|hu|id|ie|il|in|io|iq|ir|is|it|jm|jo|jp|ke|kg|kh|ki|km|kn|kp|kr|kw|ky|kz|la|lb|lc|li|lk|lr|ls|lt|lu|lv|ly|ma|mc|md|me|mg|mh|ml|mm|mn|mo|mp|mq|mr|ms|mt|mv|mw|mx|my|mz|na|nc|ne|nf|ng|ni|nl|no|np|nr|nt|nu|nz|om|pa|pe|pf|pg|ph|pk|pl|pm|pn|pr|pt|pw|py|qa|re|ro|ru|rw|sa|sb|sc|sd|se|sg|sh|si|sj|sk|sl|sm|sn|so|sr|st|su|sy|sz|tc|td|tf|tg|th|tj|tk|tm|tn|to|tp|tr|tt|tv|tw|tz|ua|ug|uk|us|uy|va|vc|ve|vg|vn|vu|wf|ws|ye|yu|za|zm|zr|zw";
      String regex = "^((https|http|ftp|rtsp|mms)?://)"  
              + "?(([0-9a-z_!~*'().&=+$%-]+: )?[0-9a-z_!~*'().&=+$%-]+@)?" 
             + "(([0-9]{1,3}\\.){3}[0-9]{1,3}" 
               + "|" 
            + "(([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]+\\.)?"  
               + "([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]\\."   
              + "("+domainRules+"))" 
              + "(:[0-9]{1,4})?"  
              + "((/?)|"  
              + "(/[0-9a-z_!~*'().;?:@&=+$,%#-]+)+/?)$";  
      Pattern pattern = Pattern.compile(regex);
      Matcher isUrl = pattern.matcher(str);
      return isUrl.matches();
  	}
}
