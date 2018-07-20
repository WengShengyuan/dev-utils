package org.wsy.devutils.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;


/**
 * 命令行执行工具(本地执行)
 * 
 * @ClassName LocalCommandExecutor
 * @author wengshengyuan
 * @Date 2017年10月9日 下午4:56:36
 * @version 1.0.0
 */
public class LocalCommandExecutor extends Observable implements CommandExecutor{

	private static final Logger logger = LoggerFactory.getLogger(LocalCommandExecutor.class);
	private String charSet = "utf8";

	public static final String UTF8 = "utf8";
	public static final String GBK = "gbk";
	public static final String GB2312 = "gb2312";
	public static final String UNICODE = "unicode";
	
	private volatile boolean exit = false;
	
	@Override
	public LocalCommandExecutor setCharset(String charSet) {
		this.charSet = charSet;
		return this;
	}

	@Override
	public Map<String, Object> exe(String cmd, String... args) throws Exception {

		if (args != null && args.length > 0) {
			for (String arg : args) {
				cmd += " " + arg;
			}
		}

		logger.info("executing command: {}", cmd);

		BufferedReader stdInput = null;
		BufferedReader stdError = null;
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(cmd);
			// Get input streams
			stdInput = new BufferedReader(new InputStreamReader(process.getInputStream(), charSet));
			stdError = new BufferedReader(new InputStreamReader(process.getErrorStream(), charSet));
			Map<String,Object> results = new HashMap<String,Object>();
			List<String> resultList = new ArrayList<String>();
			List<String> errorList = new ArrayList<String>();
			String line = null;
			while ((line = stdInput.readLine()) != null) {
				setChanged();
				notifyObservers(line);
				if(this.exit){
					results.put("success", resultList);
					results.put("error", errorList);
					return results;
				}
				resultList.add(line);
			}
			while ((line = stdError.readLine()) != null) {
				setChanged();
				notifyObservers(line);
				if(this.exit){
					results.put("success", resultList);
					results.put("error", errorList);
					return results;
				}
				errorList.add(line);
			}
			int exitValue = process.waitFor();
			logger.info("exe command exit code: {}", process.exitValue());
			results.put("success", resultList);
			results.put("error", errorList);
			results.put("exit",exitValue);
			return results;
		} catch (Exception e) {
			throw e;
		} finally {
			if (stdInput != null) {
				stdInput.close();
			}
			if (stdError != null) {
				stdError.close();
			}
			if (process != null) {
				process.destroy();
			}
		}
	}

	@Override
	public void exit() {
		this.exit = true;
	}
}
