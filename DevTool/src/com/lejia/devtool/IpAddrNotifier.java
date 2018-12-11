package com.lejia.devtool;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.Log;

public class IpAddrNotifier extends Service {
	private static final String TAG = "IpAddrNotifier";
	
	private static final long NOTIFY_INTERVAL_MIN_MILLIS = 20 * 1000;
	
	private Context mContext;
	private long mLastNotifyTimeMilli = 0;
	private NetworkConnectChangedReceiver mNetworkConnectChangedReceiver;

    @Override
    public void onCreate() {
    	super.onCreate();
    	mContext = this;
    	Log.i(TAG, "onCreate()");
    	
    	mNetworkConnectChangedReceiver = new NetworkConnectChangedReceiver();
    	IntentFilter filter = new IntentFilter();
    	filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    	filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
    	filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
    	registerReceiver(mNetworkConnectChangedReceiver, filter);
    }
	
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");
        unregisterReceiver(mNetworkConnectChangedReceiver);
    }
    
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        notifyIpAddr();
        return START_NOT_STICKY;
    }
    
    private int getAudioResId(char ch) {
    	int id;
    	switch (ch) {
    	case '0':
    		id = R.raw.digit_0;
    		break;
    	case '1':
    		id = R.raw.digit_1;
    		break;
    	case '2':
    		id = R.raw.digit_2;
    		break;
    	case '3':
    		id = R.raw.digit_3;
    		break;
    	case '4':
    		id = R.raw.digit_4;
    		break;
    	case '5':
    		id = R.raw.digit_5;
    		break;
    	case '6':
    		id = R.raw.digit_6;
    		break;
    	case '7':
    		id = R.raw.digit_7;
    		break;
    	case '8':
    		id = R.raw.digit_8;
    		break;
    	case '9':
    		id = R.raw.digit_9;
    		break;
    	default:
    		id = R.raw.dot;
    		break;
    	}
    	
    	return id;
    }
    
    private String getIpAddress() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {  
        	wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        wifiManager.getWifiState();
        int ipAddress = wifiInfo.getIpAddress();
        if (ipAddress != 0) {
        	return intToIp(ipAddress);
        } else {
        	return null;
        }
    }
    
    private String intToIp(int i) {       
        return (i & 0xFF ) + "." +
	      ((i >> 8 ) & 0xFF) + "." +
	      ((i >> 16 ) & 0xFF) + "." +
	      ( i >> 24 & 0xFF);
   }
    
    private void notifyIpAddr() {
    	Log.i(TAG, "notifyIpAddr()");
    	if (SystemClock.uptimeMillis() - mLastNotifyTimeMilli < NOTIFY_INTERVAL_MIN_MILLIS) {
    		Log.d(TAG, "notifyIpAddr() do nothing ");
    		return;
    	}
    	
    	String ipAddr = getIpAddress();
    	int len = 0;
    	char ch;
    	MediaPlayer headPlayer = null;
    	MediaPlayer tailPlayer = null;
    	MediaPlayer nextPlayer = null;
    	if (ipAddr != null) {
    		len = ipAddr.length();
    	}
    	for (int i=0; i<len; i++) {
    		ch = ipAddr.charAt(i);
    		if (tailPlayer == null) {
    			tailPlayer = MediaPlayer.create(mContext, getAudioResId(ch));
    			headPlayer = tailPlayer;
    		} else {
    			nextPlayer = MediaPlayer.create(mContext, getAudioResId(ch));
    			tailPlayer.setNextMediaPlayer(nextPlayer);
    			tailPlayer = nextPlayer;
    		}
    	}
    	
    	if (tailPlayer != null) {
        	tailPlayer.setNextMediaPlayer(null);
        	
    		MediaPlayer player = MediaPlayer.create(mContext, R.raw.ip_tip);
    		player.setNextMediaPlayer(headPlayer);
        	player.start();
        	mLastNotifyTimeMilli = SystemClock.uptimeMillis();
    	}
    }
    
    public class NetworkConnectChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                Parcelable parcelableExtra = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (null != parcelableExtra) {
                    NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                    State state = networkInfo.getState();
                    boolean isConnected = state == State.CONNECTED;
                    Log.i(TAG, "isConnected" + isConnected);
                    if (isConnected) {
                    	notifyIpAddr();
                    } 
                }
            }
        }
    }
}
