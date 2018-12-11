package com.lejia.devtool.download.logic;


/**
 * 
 * Upgrade property ext
 * 
 * @author jerome
 *
 */
public final class DlFileHelperConfig {
	
	boolean Downloadable= false; 
	int mDownloadMode;
	protected Builder mBuilder;
	
	private DlFileHelperConfig(final Builder builder){
		this.mBuilder = builder;
		Downloadable = builder.mDownable;
	}
	
	public boolean isDownloadable(){
		return Downloadable;
	}
	
	public int getDlMode(){
//		if(mBuilder == null) throw new IllegalAccessException("mBuilder must not be null.");
		return mBuilder.getMode();
	}
	
	public boolean isZipMode(){
		return mBuilder.isZipMode();
	}
	
	public boolean isPatchMode(){
		return mBuilder.isPatchMode();
	}
	
	public boolean isShMode(){
		return mBuilder.isShMode();
	}
	
	public void setMode(int mode){
		if(mBuilder != null) mBuilder.setMode(mode);
	}
	
	public static DlFileHelperConfig createDefault() {
		return new Builder().build();
	}
	
	public static class Builder {
		
		/**
		 * zip dlMode
		 */
		public static final int  Mode_ZIP = 0x01;
		/**
		 * apk dlMode
		 */
		public static final int  Mode_APK = 0x10;
		/**
		 * patch dlMode
		 */
		public static final int Mode_PATCH = 0x11;

		/**
		 * sh dlMode
		 */
		public static final int Mode_SH= 0x100;
		
		
		private boolean mDownable = false;
		private boolean mDebugCfg = false;
		
		private int mode = Mode_SH;
		
		public Builder() {}
		
		public Builder enableDownload(boolean enable){
			this.mDownable = enable;
			return this;
		}
		
		public Builder switchBuildCfg(boolean buildCfg){
			this.mDebugCfg = buildCfg;
			if(mDebugCfg){
				
			}else{
				
			}
			return this;
		}
		
		public Builder setMode(int mode){
			this.mode = mode;
			return this;
		}
		
		public int getMode(){
			return mode;
		}
		
		private boolean isZipMode(){
			return (mode == Mode_ZIP);
		}

		private boolean isPatchMode(){
			return (mode == Mode_PATCH);
		}
		
		private boolean isShMode(){
			return (mode == Mode_SH);
		}
		
		public DlFileHelperConfig build() {
			return new DlFileHelperConfig(this);
		}
		
		public boolean isDownloadable(){
			return mDownable;
		}
		
	}
	
}
