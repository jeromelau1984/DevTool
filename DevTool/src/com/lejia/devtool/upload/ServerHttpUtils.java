package com.lejia.devtool.upload;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.zip.GZIPOutputStream;

import android.util.Log;

public class ServerHttpUtils {

	protected final static String TAG = "ServerInteract";

	public static byte[] compress(String str) throws IOException {
		if (str == null || str.length() == 0) {
			return null;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GZIPOutputStream gzip = new GZIPOutputStream(out);
		gzip.write(str.getBytes());
		gzip.close();
		return out.toByteArray();
	}

	public static String doPost(String httpUrl, String httpBody) throws IOException {
		return doPost(httpUrl,httpBody.getBytes(),false);
	}
	
	public static String doGzipPost(String httpUrl, String httpBody) throws IOException {
		return doPost(httpUrl,compress(httpBody),true);
	}
	
	public static String doPost(String httpUrl, byte[] httpBody,boolean gzipFlag) throws IOException {
		String result = null;
		BufferedReader input = null;
		InputStream in = null;
		OutputStream outStream = null;
		HttpURLConnection connection = null;
		try {
			URL url = new URL(httpUrl);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestMethod("POST");
			connection.addRequestProperty("http.socket.timeout", "600000");
			connection.setRequestProperty("content-type", "text/json");
			if(gzipFlag){
				connection.setRequestProperty("Content-Encoding", "gzip");
			}
			outStream = connection.getOutputStream();
			outStream.write(httpBody);
			outStream.flush();
			outStream.close();

			int responseCode = connection.getResponseCode();

			if (responseCode == 200) {
				in = new BufferedInputStream(connection.getInputStream());
			} else {
				in = new BufferedInputStream(connection.getErrorStream());
				Log.e(TAG, httpUrl+"get error result:" + result);
				return null;
			}
			input = new BufferedReader(new InputStreamReader(in));
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = input.readLine()) != null) {
				sb.append(line);
			}
			result = sb.toString();
//			Log.d(TAG, "Get result from server:" + result);
		} catch (UnknownHostException ex) {
			ex.printStackTrace();
			Log.e(TAG, "STATUS_NETWORK_ERR getHttpReq(), UnknownHostException",
					ex);
		} catch (UnsupportedEncodingException ex) {
			Log.e(TAG,
					"UnsupportedEncodingException getHttpReq(), UnsupportedEncodingException",
					ex);
		} catch (IllegalStateException ex) {
			Log.e(TAG,
					"IllegalStateException getHttpReq(), IllegalStateException",
					ex);
		} finally {
			try {
				if (input != null) {
					input.close();
				}
				if (in != null) {
					in.close();
				}
				if (outStream != null) {
					outStream.close();
				}
				if (connection != null) {
					connection.disconnect();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e(TAG, "HttpClient execute close exception", e);
				return null;
			}
		}
		return result;
	}
	
	public static String postGzip(String str, String fileName) {
		String result = null;
		try {
			File fvedioName = new File(fileName);
			BufferedInputStream bis = new BufferedInputStream(
							new FileInputStream(fvedioName));
			URL url = new URL(str);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("content-type", "text/json");
			connection.setRequestProperty("Content-Encoding", "gzip");
			connection.addRequestProperty("FileName", android.os.Build.SERIAL +"_"+ fvedioName.getName());

			GZIPOutputStream gos = new GZIPOutputStream(
					connection.getOutputStream());
			byte[] buf = new byte[1024];
			int bytes_read;
			while((bytes_read = bis.read(buf)) != -1){
				gos.write(buf, 0, bytes_read);// 图片内容循环写
				gos.flush();
			}
			gos.close();
			bis.close();
			
			int responseCode = connection.getResponseCode();
			InputStream in = null;
			if (responseCode == 200) {
				in = new BufferedInputStream(connection.getInputStream());
			} else {
				in = new BufferedInputStream(connection.getErrorStream());
			}
			result = readInStream(in);
			return result;
		} catch (Exception e) {
			Log.e(TAG,"postGzip_error:",e);
		}
		return null;
	}
	
	public static String postFile(String str, String fileName) {
		String result = null;
		Log.e("Jerome", "postFile fileName" + fileName);
        try {
        	File f = new File(fileName);
            if(f.length() <= 0){
            	Log.e("Jerome", "postFile f.length() <= 0");
            	return result;
            }
            URL url=new URL(str);
            
            HttpURLConnection connection=(HttpURLConnection)url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.addRequestProperty("FileName", android.os.Build.SERIAL +"_"+ f.getName());
            connection.addRequestProperty("http.socket.timeout", "600000");
            connection.setRequestProperty("content-type", "text/json");
            connection.setRequestProperty("Content-Encoding", "gzip");
            GZIPOutputStream gzs = new GZIPOutputStream(connection.getOutputStream());
            InputStream bis = new FileInputStream(fileName);
            
            byte[]bytes=new byte[1024];
            int numReadByte=0;
    		while((numReadByte=bis.read(bytes,0,1024))>0)
            {
    			gzs.write(bytes, 0, numReadByte);
            }
    		bis.close();
    		gzs.flush();
    		gzs.close();
    		result = readInStream(connection.getInputStream());
			return result;
        } catch (Exception e) {
        	Log.e(TAG,"postFile_error:",e);
        }
        return result;
    }

	public static String readInStream(InputStream in) {
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(in).useDelimiter("\\A");
		return scanner.hasNext() ? scanner.next() : "";
	}
}
