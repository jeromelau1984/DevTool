package com.lejia.devtool;

import java.io.File;
import java.io.IOException;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import android.bluetooth.BluetoothAdapter;
import android.os.Message;
import android.os.Handler;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import com.mediatek.telephony.TelephonyManagerEx;

public class DevService extends Service {
	private static final String TAG = "DevService";

    private static final int RETRY_TIMES = 200;
    private static final int RETRY_SLEEP_TIME = 500;
    private static final int INTERRUPT_RETRY = 0x01;

    // switch to init service center number
	private static final boolean FLAG_INIT_SC = false;

	private Context mContext;
    private Thread  retryThread;
    private Handler mHandler;

    @Override
    public void onCreate() {
    	super.onCreate();
    	mContext = this;
    	Log.i(TAG, "onCreate()");

        mHandler = new Handler(){
               public void handleMessage(Message msg){
                       Log.i(TAG, "Jerome join step1 ...");
                       try {
                               if (msg != null && msg.what == INTERRUPT_RETRY) {
                                       if (retryThread != null) {
                                               retryThread.interrupt();
                                               retryThread = null;
                                       }
                               }
                       } catch (Exception e) {
                               Log.e(TAG, "Jerome handleMessage setScanMode e : " + e.toString());
                       }
               }
       };

	    if (retryThread == null) {
		    retryThread = new Thread(){
			    public void run(){
				    doRetrySetScanMode();
				    if(FLAG_INIT_SC)
				        initServiceCenterNumber();
			    }
		    };
		    retryThread.start();
	    }
    }

    private void doRetrySetScanMode(){
	try {
		BluetoothAdapter btadapter = BluetoothAdapter.getDefaultAdapter();
		Log.i(TAG, "Jerome join...");
		if (btadapter != null) {
			boolean btEnable = btadapter.enable();
			Log.i("Jerome", "settings btadapter.btEnable : " + btEnable);

			int mScanMode = btadapter.getScanMode();
			if (mScanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
				if (mHandler != null) {
					mHandler.obtainMessage(INTERRUPT_RETRY);
				}
				return;
			}

			int retryTime = 0;
			while (retryTime++ <= RETRY_TIMES) {
				boolean mSetScanModeRlt = btadapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
				Log.i("Jerome", "settings btadapter.setScanMode : " + mSetScanModeRlt + " , try " + retryTime + " times.");
				if (mSetScanModeRlt) {
					if (mHandler != null) {
						mHandler.obtainMessage(INTERRUPT_RETRY);
					}
					break;
				} 
				try {
					Thread.sleep(RETRY_SLEEP_TIME);
				} catch (InterruptedException e) {
					Log.e(TAG, "Jerome retry setScanMode e : " + e.toString());
				}	
			}
			
		}
	} catch (Exception e) {
		Log.e(TAG, "Jerome setScanMode e : " + e.toString());
	}
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        runScript();
        return START_NOT_STICKY;
    }
    
    private void runScript() {
    	if (Utils.isFileExist(Utils.PREDEFINE_SCRIPT_FILE)) {
            File file = new File(Utils.PREDEFINE_SCRIPT_FILE);
            file.setExecutable(true, false);
    		
            try {
            	Process proc = Runtime.getRuntime().exec("sh " + Utils.PREDEFINE_SCRIPT_FILE);
            	Toast.makeText(mContext, "[NOTE] Exec: " + Utils.PREDEFINE_SCRIPT_FILE, Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
    	}
    }
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");

	    try {
		    if (retryThread != null) {
			    retryThread.interrupt();
			    retryThread = null;
		    }			
	    } catch (Exception e) {
		    Log.e(TAG, "Jerome onDestroy e : " + e.toString());
	    }
    }

    private void initServiceCenterNumber() {
       Log.i(TAG, "initServiceCenterNumber --in--");

       String scNumber = null;
       int subId = Utils.getDefaultDataSubId(mContext);

       Log.i(TAG, "initServiceCenterNumber: subId = " + subId);

        for(int i = 0; i < RETRY_TIMES; i++) {
            Log.i(TAG, "initServiceCenterNumber - try:" + i);

           scNumber = getServiceCenter(subId);
           if(scNumber != null) {
                break;
            }

           /** TODO: temply not to be needed. need redesign the thread function.
            *  sync with bt thread function.
            */
           /* setServiceCenter(subId, scNumber);*/

            try {
		        Thread.sleep(RETRY_SLEEP_TIME * 10);
	        } catch (InterruptedException e) {
		        Log.e(TAG, "initServiceCenterNumber e : " + e.toString());
	        }
        }

       Log.i(TAG, "initServiceCenterNumber: subId = " + subId + "; scNumber = " + scNumber + "--out--");
    }

    private String getServiceCenter(int subId) {
        Log.i(TAG, "getServiceCenter: subId = " + subId);
        String scNumber = null;

        if (Utils.MTK_C2K_SUPPORT && Utils.isUSimType(subId) &&
            /// M: modify for OP09 feature, not allow SMS center in WCDMA.
            !Utils.isCSIMInGsmMode(subId)) {
            Log.i(TAG, "getServiceCenter: WCDMA");
        } else {
            Log.i(TAG, "getServiceCenter: getScAddressWithErroCode");
            Bundle result = TelephonyManagerEx.getDefault().getScAddressWithErroCode(subId);

            if (result != null
                    && result.getByte(TelephonyManagerEx.GET_SC_ADDRESS_KEY_RESULT) == TelephonyManagerEx.ERROR_CODE_NO_ERROR) {
                scNumber = (String) result
                        .getCharSequence(TelephonyManagerEx.GET_SC_ADDRESS_KEY_ADDRESS);
                Log.i(TAG, "getScAddress is: " + scNumber);
            } else {
                Log.i(TAG, "getScAddress error: " + result);
            }
        }

        return scNumber;
    }

    private void setServiceCenter(final int subId, final String scNumber) {
        Log.i(TAG, "subId is: " + subId);
        Log.i(TAG, "setScAddress is: " + scNumber);

        new Thread(new Runnable() {
            public void run() {
                TelephonyManagerEx.getDefault().setScAddress(subId, scNumber);
            }
        }).start();
    }
}
