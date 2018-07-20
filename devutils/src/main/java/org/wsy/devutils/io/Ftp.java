package org.wsy.devutils.io;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Ftp传输客户端
 * 
 * @ClassName Ftp
 * @author wengshengyuan
 * @Date 2017年11月17日 下午3:12:39
 * @version 1.0.0
 */
public class Ftp {

	public static boolean uploadFile(String host, int port, String username, String password, File sourceFile)
			throws Exception {
		FTPClient ftp = new FTPClient();
		boolean result = false;
		if(!sourceFile.exists()) {
			throw new Exception("file ["+sourceFile.getName()+"] doesn't exist");
		}
		try (FileInputStream input = new FileInputStream(sourceFile)) {
			int reply;
			ftp.connect(host, port);// 连接FTP服务器
			// 如果采用默认端口，可以使用ftp.connect(host)的方式直接连接FTP服务器
			ftp.login(username, password);// 登录
			
			reply = ftp.getReplyCode();
			
			if (!FTPReply.isPositiveCompletion(reply)) {
				ftp.disconnect();
				return result;
			}
			ftp.enterLocalPassiveMode();
//			ftp.makeDirectory("/");x	`
//			ftp.changeWorkingDirectory("/");
//			// 切换到上传目录
//			if (!ftp.changeWorkingDirectory(basePath + filePath)) {
//				// 如果目录不存在创建目录
//				String[] dirs = filePath.split("/");
//				String tempPath = basePath;
//				for (String dir : dirs) {
//					if (null == dir || "".equals(dir))
//						continue;
//					tempPath += "/" + dir;
//					if (!ftp.changeWorkingDirectory(tempPath)) {
//						if (!ftp.makeDirectory(tempPath)) {
//							return result;
//						} else {
//							ftp.changeWorkingDirectory(tempPath);
//						}
//					}
//				}
//			}
			// 设置上传文件的类型为二进制类型
			ftp.setFileType(FTP.BINARY_FILE_TYPE);
			// 上传文件
			if (!ftp.storeFile(sourceFile.getName(), input)) {
				return result;
			}
			ftp.logout();
			result = true;
			return result;
		} catch (IOException e) {
			throw new Exception("ftp transfer error.",e.getCause());
		} finally {
			if (ftp.isConnected()) {
				try {
					ftp.disconnect();
				} catch (IOException ioe) {
				}
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println(Ftp.uploadFile("10.2.221.108", 21, "tlzgs", "sjtstlzgs!@34", new File("C://20171117090000_20171117093000.log")));
	}

}
