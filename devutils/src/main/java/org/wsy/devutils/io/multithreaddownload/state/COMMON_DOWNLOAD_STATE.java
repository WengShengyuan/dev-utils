package org.wsy.devutils.io.multithreaddownload.state;

public class COMMON_DOWNLOAD_STATE {

	/* 下载状态 */
	public static final int INITED = 0; // 初始
	public static final int SUCCESS = 2; // 下载成功
	public static final int WORKING = 1; // 正在下载
	public static final int END = 3; // 终止下载
	public static final int FAIL = -1; // 下载失败
	
}
