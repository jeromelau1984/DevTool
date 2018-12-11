package com.lejia.devtool.download;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.util.Log;

public class ThreadPoolManager {

	private final String TAG = ThreadPoolManager.class.getName();

	private int poolSize;
	private static final int MIN_POOL_SIZE = 1;
	private static final int MAX_POOL_SIZE = 10;

	private ExecutorService threadPool;

	private LinkedList<Runnable> asyncTasks;

	private int type;
	public static final int TYPE_FIFO = 0;
	public static final int TYPE_LIFO = 1;

	private PoolWorker mPoolWorker;

	private static int POOLS_COUNTS = 2;

	private static ThreadPoolManager poolManager;

	public synchronized static ThreadPoolManager getInstance() {
		if (poolManager == null) {
			synchronized (ThreadPoolManager.class) {
				if (poolManager == null) {
					poolManager = new ThreadPoolManager(
							ThreadPoolManager.TYPE_FIFO, POOLS_COUNTS);
				}
			}
		}
		return poolManager;
	}

	private ThreadPoolManager(int type, int poolSize) {
		this.type = (type == TYPE_FIFO) ? TYPE_FIFO : TYPE_LIFO;

		if (poolSize < MIN_POOL_SIZE)
			poolSize = MIN_POOL_SIZE;
		if (poolSize > MAX_POOL_SIZE)
			poolSize = MAX_POOL_SIZE;
		this.poolSize = poolSize;

		threadPool = Executors.newFixedThreadPool(this.poolSize);
		asyncTasks = new LinkedList<Runnable>();
		start();
	}

	public synchronized void shutDownThreadPool() {
		if (poolManager != null) {
			poolManager.shutdownNow();
			poolManager = null;
		}
	}

	public void addAsyncTask(Runnable task) {
		synchronized (asyncTasks) {
			asyncTasks.addLast(task);
			asyncTasks.notify();
		}
	}

	private Runnable getAsyncTask() {
		synchronized (asyncTasks) {
			if (asyncTasks.size() > 0) {
				Runnable task = (this.type == TYPE_FIFO) ? asyncTasks
						.removeFirst() : asyncTasks.removeLast();

				return task;
			}
		}
		return null;
	}

	private void start() {
		if (mPoolWorker == null) {
			mPoolWorker = new PoolWorker();
			mPoolWorker.start();
		}
	}
	
	private void shutdownNow() {
		clearTasks();

		if (threadPool != null) {
			threadPool.shutdownNow();
			threadPool = null;
		}
		if (mPoolWorker != null) {
			mPoolWorker.stopLoop();
			mPoolWorker = null;
		}
	}

	private void clearTasks() {
		if (asyncTasks != null) {
			synchronized (asyncTasks) {
				asyncTasks.clear();
			}
		}
	}

	private class PoolWorker extends Thread {
		private boolean isLoop = true;

		@Override
		public void run() {

			while (isLoop) {
				synchronized (asyncTasks) {
					Runnable task = getAsyncTask();
					if (task == null) {
						try {
							Log.v(TAG, "PoolWorker wait");
							asyncTasks.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

					} else {
						if (threadPool != null) {
						    Log.v(TAG, "PoolWorker execute");
							threadPool.execute(task);
						}
					}
				}
			}
		}

		public void stopLoop() {
			isLoop = false;
			try {
				interrupt();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
