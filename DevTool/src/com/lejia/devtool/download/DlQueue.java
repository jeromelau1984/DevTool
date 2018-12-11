package com.lejia.devtool.download;

import java.util.LinkedList;

import android.util.Log;

public class DlQueue {
	
    private static final String TAG =  DlQueue.class.getSimpleName();
    
	@SuppressWarnings("rawtypes")
	public LinkedList list = new LinkedList();
	
    @SuppressWarnings("unchecked")
	public boolean insert(Object v){
        if(!list.contains(v)){
            Log.i(TAG, "insert : " + v);
            return list.offer(v);
    		}
    		return false;
    }
    
    public Object peek(){
    		Object obj = list.peek();
        Log.i(TAG, "peek : " + obj);
        return obj;
    }

    public Object element(){
        return list.element();
    }
    
    public Object poll(){
        return list.poll();
    }

    public Object remove(){
    		Object obj = list.remove();
        Log.i(TAG, "remove : " + obj);
        return obj;
    }
    
    public boolean isEmpty(){
        return list.isEmpty();
    }
    
    public void clear(){
    		if(null != list) list.clear();
    		list = null;
    }
    
    public int size(){
    		return list.size();
    }
    
}
