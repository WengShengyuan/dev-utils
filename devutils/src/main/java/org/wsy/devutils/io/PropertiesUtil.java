package org.wsy.devutils.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * 属性文件读取工具
 * @ClassName PropertiesUtil
 * @author wengshengyuan
 * @Date 2017年9月20日 上午9:28:43
 * @version 1.0.0
 */
public class PropertiesUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);
	
	public static Properties readProperties(String filePath) throws Exception {
		logger.info("read properties from : "+filePath);
		try (InputStream stream = new FileInputStream(new File(filePath))){
			Properties properties = new Properties();
			properties.load(stream);
			return properties;
		} catch (Exception e) {
			throw e;
		} 
	}
	
	
	public static Properties readProperties(InputStream inputStream) throws Exception {
		logger.info("read properties from inputStream...");
		try {
			Properties properties = new Properties();
			properties.load(inputStream);
			return properties;
		} catch (Exception e) {
			throw e;
		} 
	}

}
