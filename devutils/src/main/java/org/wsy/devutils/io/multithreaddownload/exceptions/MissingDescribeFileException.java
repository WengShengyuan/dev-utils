package org.wsy.devutils.io.multithreaddownload.exceptions;

import java.io.File;

public class MissingDescribeFileException extends Exception {

	private static final long serialVersionUID = 4400089665938342422L;

	public MissingDescribeFileException(File missionDescribeFile){
		super("MissionDescribeFile "+ missionDescribeFile.getAbsolutePath() +" not found");
	}
}
