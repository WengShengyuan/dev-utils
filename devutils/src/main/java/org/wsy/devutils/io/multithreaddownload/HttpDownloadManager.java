package org.wsy.devutils.io.multithreaddownload;

import com.rails.wifi.utildevutilscollection.io.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 下载管理器
 * 
 * @ClassName HttpDownloadManager
 * @author wengshengyuan
 * @Date 2017年12月4日 下午1:40:17
 * @version 1.0.0
 */
public class HttpDownloadManager {

	private static final Logger logger = LoggerFactory.getLogger(HttpDownloadManager.class);
	private static volatile HttpDownloadManager manager;
	private Map<String, HttpDownloadMission> missionMap;
	private int limitSpeed = 200;
	
	@Override
	public String toString() {
		return "HttpDownloadManager [missionMap=" + missionMap + "]";
	}

	public static HttpDownloadManager getInstance() {
		if (manager == null) {
			synchronized (HttpDownloadManager.class) {
				if (manager == null) {
					manager = new HttpDownloadManager();
				}
			}
		}
		return manager;
	}

	private HttpDownloadManager() {
		this.missionMap = new ConcurrentHashMap<String, HttpDownloadMission>();
		
		try(InputStream fis = HttpDownloadManager.class.getClassLoader().getResourceAsStream("util-dev-utils-collection.properties")) {
			String tempSpeed = PropertiesUtil.readProperties(fis).getProperty("io.http.downloader.limitspeed");
			if(tempSpeed != null && tempSpeed != "")
				limitSpeed = Integer.valueOf(tempSpeed);
		} catch (Exception e) {
			logger.error("fail to read properties set to default. {}",e.getMessage());
		}
	}

	public Map<String, HttpDownloadMission> getMissions() {
		return this.missionMap;
	}

	public String createMission(String url, File destFile, String fileMd5) throws Exception {
		logger.info("准备创建任务 url={}, fileMd5={}, destFile={},limitSpeed={}", url, fileMd5, destFile.getAbsolutePath(),"");
		String missionId = fileMd5;
		if (missionMap.get(missionId) != null) {
			logger.info("任务id:{}已经存在，直接返回...", fileMd5);
			return missionId;
		}

		// 执行下载任务
		synchronized (missionMap) {
			HttpDownloadMission mission = new HttpDownloadMission(url, missionId, destFile, fileMd5,limitSpeed);
			missionMap.put(missionId, mission);
		}
		return missionId;
	}

	
	public void deleteMission(String missionId) throws Exception {
		synchronized (missionMap) {
			missionMap.get(missionId).deleteTmpFile();
 			missionMap.remove(missionId);
		}
	}
	
	public void startMission(String missionId) throws Exception {
		synchronized (missionMap) {
			missionMap.get(missionId).startMission();			
		}
	}

	public void stopMission(String missionId) throws Exception { // 根据missionId作废下载任务,修改配置文件中的状态信息
		missionMap.get(missionId).stopMission();
	}

	public int getMissionState(String missionId) {
		return missionMap.get(missionId).getMissionState().getMissionState();
	}

	public HttpDownloadMission getMission(String missionId) {
		return missionMap.get(missionId);
	}
	
}
