package org.wsy.devutils.io.multithreaddownload.exceptions;

public class MalformedDescribeFileException extends Exception {

	private static final long serialVersionUID = 4400089665938342422L;

	public MalformedDescribeFileException(String content){
		super("mission describe file content: "+ content +" malformed");
	}
}
