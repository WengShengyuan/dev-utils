package org.wsy.devutils.io.multithreaddownload.state;

import java.io.Serializable;

public class DownloaderState implements Serializable {

	private static final String SPLITTER = "@";
	private static final long serialVersionUID = -6878154457309085628L;
	
	public DownloaderState() {}
	
	public DownloaderState(String line) throws IllegalArgumentException {
		String[] part = line.split(SPLITTER);
		if(part.length != 2) {
			throw new IllegalArgumentException("状态 "+line+" 长度不为2");
		}
		try {
			this.downloaderId = Integer.valueOf(part[0]);
			this.downloaderState = Integer.valueOf(part[1]);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	@Override
	public String toString() {
		return "DownloaderState [downloaderId=" + downloaderId + ", downloaderState=" + downloaderState + "]";
	}

	int downloaderId;
	int downloaderState;
	public int getDownloaderId() {
		return downloaderId;
	}
	public int getDownloaderState() {
		return downloaderState;
	}
	public void setDownloaderId(int downloaderId) {
		this.downloaderId = downloaderId;
	}
	public void setDownloaderState(int downloaderState) {
		this.downloaderState = downloaderState;
	}
	
	public String toStateLine(){
		return String.format("%s%s%s", this.downloaderId,SPLITTER,this.downloaderState);
	}
	
}
