package com.lejia.devtool.download.logic;

import java.util.concurrent.Semaphore;

import com.lejia.devtool.download.DlListener;
import com.lejia.devtool.download.DlRemoteFile;

public abstract class BaseRunnable implements Runnable, DlConstants{

	protected volatile boolean isBreak = false;
	
	protected long startTime = System.currentTimeMillis();
	protected long percent = 0;
	protected long endTime = 0;
	protected DlListener listener;
	protected long usedTime = 0;
	protected volatile boolean isRunning = false;
	protected String mDownloadAbsolutePath;
	
	protected Semaphore unZipLock = new Semaphore(1);
	
	protected boolean isUnZipLockFair(){
		return unZipLock.isFair();
	}

	protected void releaseSemaphore(){
		if(null != unZipLock)
			unZipLock.release();
	}
	
	@Override
	public void run() {
		int result = 0;
		startTime = System.currentTimeMillis();
		while (true) {
			if(isBreak) break;
			result = download();
			if (result > 0) {
				continue;
			} else {
				break;
			}
		}
	}
	
	protected boolean isRun() {
		return isRunning;
	}
	
	public void changeNeedRun() {
		this.isBreak = true;
	}
	
	/**
	 * Waiting for prepare download data
	 */
	public void waiting(){
		this.isRunning = false;
	}
	
	public void prepare(){
		this.isRunning = true;
	}
	
	protected void release(){
		if(null != listener) listener = null;
	}
	
	protected abstract int download(BaseRunnable runnable , BaseTransact trans);
	
	protected abstract int download();
	
	protected abstract void init(DlRemoteFile zip);
	
	protected abstract void transact(BaseTransact btact);
	
}
