package org.wsy.devutils.io.multithreaddownload.exceptions;

public class MissionExistException extends Exception {

	private static final long serialVersionUID = 4400089665938342422L;

	public MissionExistException(String missionId){
		super("HttpDownloadMission "+ missionId +" already running");
	}
}
