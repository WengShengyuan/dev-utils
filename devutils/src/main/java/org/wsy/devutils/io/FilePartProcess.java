package org.wsy.devutils.io;

import org.wsy.devutils.io.wrapper.FilePartProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 文件分块处理
 * 
 * @author WengShengyuan
 *
 */
public class FilePartProcess {

	private static final Logger logger = LoggerFactory.getLogger(FilePartProcess.class);

	/**
	 * 文件拆分
	 * 
	 * @param fullFile
	 * @param partLength
	 * @param toFolder
	 * @return
	 * @throws Exception
	 */
	public static List<FilePartProperties> split(File fullFile, int partLength, File toFolder) throws Exception {
		if (!fullFile.exists())
			throw new Exception("source file does'n exist");
		if (!toFolder.exists())
			toFolder.mkdirs();
		if (!toFolder.isDirectory())
			throw new Exception("file:" + toFolder.getAbsolutePath() + " is not a directory");
		String fullFileMd5 = FileMd5Check.getMD5Checksum(fullFile);
		logger.debug("fullFile md5 check:{}",fullFileMd5);
		
		logger.info("文件拆分(length:{}) {} -> {}...", partLength, fullFile.getAbsoluteFile(), toFolder.getAbsoluteFile());
		long fullLength = fullFile.length();
		
		List<FilePartProperties> parts = new LinkedList<FilePartProperties>();
		FileInputStream fis = null;
		try{
			fis = new FileInputStream(fullFile);
			byte[] buffer = new byte[partLength];
			int len,count=0;
			while((len=fis.read(buffer))!=-1){
				FilePartProperties part = new FilePartProperties();
				String partFilePath = toFolder.getAbsolutePath()+File.separator+fullFile.getName()+".part"+count;
				File partFile = new File(partFilePath);
				if(partFile.exists())
					partFile.delete();
				FileOutputStream fos = null;
				part.setFullLength(fullLength);
				part.setPartFilePath(partFilePath);
				part.setPartLength(partFile.length());
				part.setPartOrder(count);
				part.setFullMd5(fullFileMd5);
				try {
					fos = new FileOutputStream(partFile);
					fos.write(buffer,0,len);
					fos.flush();
					fos.close();
					part.setPartLength(partFile.length());
					String partFileMd5 = FileMd5Check.getMD5Checksum(partFile);
					logger.debug("partFile No{} Md5 -> {} ... ",count,partFileMd5);
					part.setPartFileMd5(partFileMd5);
					parts.add(part);
				} catch (Exception e) {
					throw e;
				} finally {
					if(null != fos)
						fos.close();
				}
				count++;
			}
			
		} catch (Exception e) {
			throw new Exception("分割文件失败.",e.getCause());
		} finally {
			if (null != fis)
				fis.close();
		}
		return parts;
	}

	/**
	 * 文件整合
	 * 
	 * @param parts
	 * @param destFile
	 * @return
	 * @throws Exception
	 */
	public static File merge(List<FilePartProperties> parts, File destFile) throws Exception {
		if (parts == null || parts.size() < 1)
			return null;
		Collections.sort(parts);
		logger.info("整合{}个子文件   -> {}... ", parts.size(), destFile.getAbsoluteFile());
		long fileLengthSum = 0L;
		for (FilePartProperties part : parts) {
			fileLengthSum += part.getPartLength();
		}
		if (fileLengthSum != parts.get(0).getFullLength())
			throw new Exception("fileLength sum doesn't match.");

		logger.debug("验证子文件...");
		for (FilePartProperties part : parts) {
			File partFile = new File(part.getPartFilePath());
			if (!partFile.exists())
				throw new Exception("file:" + part.getPartFilePath() + " not exist");
			if (partFile.length() != part.getPartLength())
				throw new Exception("file length for file:" + part.getPartFilePath() + " doesn't match.");
			String partFileMd5 = FileMd5Check.getMD5Checksum(partFile);
			logger.debug("partFile {} md5:{}", part.getPartFilePath(), partFileMd5);
			if (!partFileMd5.equals(part.getPartFileMd5()))
				throw new Exception("md5 for file:" + part.getPartFilePath() + " doesn't match.");
		}
		logger.debug("子文件验证完成,开始拼接...");
		if (destFile.exists())
			destFile.delete();

		byte[] buffer = new byte[1024];
		FileOutputStream writer = null;
		FileInputStream reader = null;
		try {
			writer = new FileOutputStream(destFile);
			int readCount = 0;
			for (FilePartProperties part : parts) {
				try {
					reader = new FileInputStream(new File(part.getPartFilePath()));
					while ((readCount = reader.read(buffer)) != -1) {
						writer.write(buffer,0,readCount);
					}
					reader.close();
				} catch (Exception e) {
					throw e;
				} finally {
					if(null != reader)
						reader.close();
				}
			}
			return destFile;
		} catch (Exception e) {
			throw new Exception("合并文件失败", e.getCause());
		} finally {
			if (writer != null)
				writer.close();
		}
	}
}
