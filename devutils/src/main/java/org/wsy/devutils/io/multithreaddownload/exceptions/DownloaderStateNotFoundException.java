package org.wsy.devutils.io.multithreaddownload.exceptions;

public class DownloaderStateNotFoundException extends Exception {

	private static final long serialVersionUID = 4400089665938342422L;

	public DownloaderStateNotFoundException(int downloaderId){
		super("downloaderState "+ downloaderId +" not found");
	}
}
