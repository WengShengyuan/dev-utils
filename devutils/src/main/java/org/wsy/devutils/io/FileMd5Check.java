package org.wsy.devutils.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * 文件MD5校验
 * @ClassName FileMd5Check
 * @author wengshengyuan
 * @Date 2017年9月20日 上午9:35:35
 * @version 1.0.0
 */
public class FileMd5Check {
	
	/**
	 * 生成MD5串
	 * @param file 文件File
	 * @return MD5串
	 * @throws Exception
	 */
	public static String getMD5Checksum(File file) throws Exception {
		byte[] b = createChecksum(file);
		String result = "";

		for (int i = 0; i < b.length; i++) {
			result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
		}
		return result;
	}
	
	private static byte[] createChecksum(File file) throws Exception {
		
		InputStream fis = new FileInputStream(file);

		byte[] buffer = new byte[1024];
		MessageDigest complete = MessageDigest.getInstance("MD5");
		int numRead;

		do {
			numRead = fis.read(buffer);
			if (numRead > 0) {
				complete.update(buffer, 0, numRead);
			}
		} while (numRead != -1);

		fis.close();
		return complete.digest();
	}

}
