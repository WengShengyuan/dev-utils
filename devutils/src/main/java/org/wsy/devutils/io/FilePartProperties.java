package org.wsy.devutils.io;

import java.io.Serializable;

/**
 * 文件分块描述
 * @author WengShengyuan
 *
 */
public class FilePartProperties implements Serializable,Comparable<FilePartProperties>{

	private static final long serialVersionUID = 7192883957225376793L;
	
	private String partFilePath;//块路径
	private String partFileMd5;//块md5
	private int partOrder;//块顺序
	private long partLength;//块大小
	private long fullLength;//完整文件大小
	private String fullMd5;//完整文件Md5
	
	@Override
	public String toString() {
		return "FilePartProperties [partFilePath=" + partFilePath + ", partFileMd5=" + partFileMd5 + ", partOrder="
				+ partOrder + ", partLength=" + partLength + ", fullLength=" + fullLength + ", fullMd5=" + fullMd5
				+ "]";
	}
	
	public int getPartOrder() {
		return partOrder;
	}

	public void setPartOrder(int partOrder) {
		this.partOrder = partOrder;
	}

	public String getPartFilePath() {
		return partFilePath;
	}
	public void setPartFilePath(String partFilePath) {
		this.partFilePath = partFilePath;
	}
	public String getPartFileMd5() {
		return partFileMd5;
	}
	public void setPartFileMd5(String partFileMd5) {
		this.partFileMd5 = partFileMd5;
	}
	public String getFullMd5() {
		return fullMd5;
	}
	public void setFullMd5(String fullMd5) {
		this.fullMd5 = fullMd5;
	}
	public long getPartLength() {
		return partLength;
	}
	public void setPartLength(long partLength) {
		this.partLength = partLength;
	}
	public long getFullLength() {
		return fullLength;
	}
	public void setFullLength(long fullLength) {
		this.fullLength = fullLength;
	}

	@Override
	public int compareTo(FilePartProperties o) {
		return this.partOrder - o.getPartOrder();
	}
}
