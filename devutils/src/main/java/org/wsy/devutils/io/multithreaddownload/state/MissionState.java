package org.wsy.devutils.io.multithreaddownload.state;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MissionState implements Serializable {

	// 如 10#2#asdkfsdfsdfsdf#...
	private static final String SPLITTER = "#";
	private static final long serialVersionUID = 6312582685107615240L;
	int blockCount;
	String missionId;
	int missionState;
	boolean canContinue;
	Map<String, DownloaderState> downloaderStates;

	public int getBlockCount() {
		return blockCount;
	}

	public String getMissionId() {
		return missionId;
	}

	public int getMissionState() {
		return missionState;
	}

	public Map<String, DownloaderState> getDownloaderStates() {
		return downloaderStates;
	}

	public void setBlockCount(int blockCount) {
		this.blockCount = blockCount;
	}

	public void setMissionId(String missionId) {
		this.missionId = missionId;
	}

	public void setMissionState(int missionState) {
		this.missionState = missionState;
	}

	public boolean isCanContinue() {
		return canContinue;
	}

	public void setCanContinue(boolean canContinue) {
		this.canContinue = canContinue;
	}

	public void setDownloaderStates(Map<String, DownloaderState> downloaderStates) {
		this.downloaderStates = downloaderStates;
	}

	public boolean isAllFinished() {
		for (DownloaderState state : downloaderStates.values()) {
			if (state.getDownloaderState() != COMMON_DOWNLOAD_STATE.SUCCESS
					&& state.getDownloaderState() != COMMON_DOWNLOAD_STATE.FAIL){
				return false;
			}
		}
		return true;
	}

	public boolean isAllSuccess() {
		for (DownloaderState state : downloaderStates.values()) {
			if (state.getDownloaderState() != COMMON_DOWNLOAD_STATE.SUCCESS){
				return false;
			}
		}
		return true;
	}

	public int getDownloadingCount() {
		int count = 0;
		for (DownloaderState state : downloaderStates.values()) {
			if (state.getDownloaderState() == COMMON_DOWNLOAD_STATE.WORKING)
				count++;
		}
		return count;
	}

	public int getSuccessCount() {
		int count = 0;
		for (DownloaderState state : downloaderStates.values()) {
			if (state.getDownloaderState() == COMMON_DOWNLOAD_STATE.SUCCESS)
				count++;
		}
		return count;
	}

	public int getFailCount() {
		int count = 0;
		for (DownloaderState state : downloaderStates.values()) {
			if (state.getDownloaderState() == COMMON_DOWNLOAD_STATE.FAIL)
				count++;
		}
		return count;
	}

	public List<DownloaderState> getFails() {
		List<DownloaderState> states = new ArrayList<DownloaderState>();
		for (DownloaderState state : downloaderStates.values()) {
			if (state.getDownloaderState() == COMMON_DOWNLOAD_STATE.FAIL) {
				states.add(state);
			}
		}
		return states;
	}

	public MissionState() {
		downloaderStates = new ConcurrentHashMap<String, DownloaderState>();
	}

	public MissionState(int blockCount, int missionState, String missionId) {
		downloaderStates = new ConcurrentHashMap<String, DownloaderState>();
		this.blockCount = blockCount;
		this.missionId = missionId;
		this.missionState = missionState;
		this.canContinue = true;
	}

	public MissionState(String line) throws IllegalArgumentException {
		downloaderStates = new ConcurrentHashMap<String, DownloaderState>();
		String[] parts = line.split(SPLITTER);
		if (parts.length < 4) {
			throw new IllegalArgumentException("任务描述分割长度小于最小长度");
		}
		try {
			this.blockCount = Integer.valueOf(parts[0]);
			this.missionState = Integer.valueOf(parts[1]);
			this.missionId = parts[2];
			this.canContinue = Boolean.valueOf(parts[3]);
			if (parts.length > 4) {
				// 第三个之后的部分为downloader状态
				downloaderStates = new HashMap<String, DownloaderState>();
				for (int i = 4; i < parts.length; i++) {
					downloaderStates.put(i+"", new DownloaderState(parts[i]));
				}
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	public synchronized String toStateLine() {
		synchronized (downloaderStates) {
			StringBuilder sBuilder = new StringBuilder();
			sBuilder.append(this.blockCount).append(SPLITTER).append(this.missionState).append(SPLITTER)
					.append(this.missionId).append(SPLITTER).append(this.canContinue);
			if (this.downloaderStates == null || this.downloaderStates.values() == null
					|| downloaderStates.values().size() < 1) {
				return sBuilder.toString();
			} else {
				for (DownloaderState state : downloaderStates.values()) {
					sBuilder.append(SPLITTER).append(state.toStateLine());
				}
				return sBuilder.toString();
			}
		}
	}

	/**
	 * 更新内存中的downloader的状态
	 * 
	 * @param downloaderId
	 * @param downloaderState
	 * @throws Exception
	 */
	public synchronized void updateDownloaderState(int downloaderId, int downloaderState) throws Exception {
		synchronized (downloaderStates) {
			if (downloaderStates.get(downloaderId) == null) {
				DownloaderState newState = new DownloaderState();
				newState.setDownloaderId(downloaderId);
				newState.setDownloaderState(downloaderState);
				downloaderStates.put(downloaderId+"", newState);
			} else {
				DownloaderState state = downloaderStates.get(downloaderId);
				state.setDownloaderState(downloaderState);
				downloaderStates.put(downloaderId+"", state);
			}			
		}
	}
}
