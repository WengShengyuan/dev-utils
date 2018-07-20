package org.wsy.devutils.system;


import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/**
 * 命令行执行工具(远程执行)
 * @ClassName RemoteCommandExecutor
 * @author wengshengyuan
 * @Date 2017年10月9日 下午5:49:57
 * @version 1.0.0
 */
public class RemoteCommandExecutor extends Observable implements CommandExecutor {

	private static final Logger logger = LoggerFactory.getLogger(RemoteCommandExecutor.class);
	
	private String host;
	private String user;
	private String password;
	
	Connection connection;
	Session session;
	boolean authed=false;
	
	private volatile boolean exit = false;
	
	private String charSet = "utf8";
	
	public static final String UTF8 = "utf8";
	public static final String GBK = "gbk";
	public static final String GB2312 = "gb2312";
	public static final String UNICODE = "unicode";
	
	public RemoteCommandExecutor(String host,String user,String password) {
		this.host = host;
		this.user = user;
		this.password = password;
	}
	
	@Override
	public CommandExecutor setCharset(String charSet) {
		this.charSet = charSet;
		return this;
	}
	
	@Override
	public Map<String, Object> exe(String cmd,String... args) throws Exception {
		BufferedReader stdInput = null;
		BufferedReader stdError = null;
		
		try {
			logger.info("establishing remote connectiong ... -> {} : {} {}",host,cmd,args);
			logger.debug("connection to remote server -> {}",host);
			connection = new Connection(host);
			connection.connect();
			logger.debug("auth ssh -> user={}, password={}",user,password);
			authed = connection.authenticateWithPassword(user, password);
			if(!authed) {
				throw new Exception("fail to authenticate, auth failed.");
			}
			logger.debug("auth result <- {}",authed);
			session = connection.openSession();
			if (args != null && args.length > 0) {
				for (String arg : args) {
					cmd += " " + arg;
				}
			}
			logger.info("start executing command: {}",cmd);
			session.execCommand(cmd, this.charSet);
			stdInput = new BufferedReader(new InputStreamReader(session.getStdout(),charSet));
			stdError = new BufferedReader(new InputStreamReader(session.getStderr(),charSet));
			Map<String, Object> results = new HashMap<String,Object>();
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
			results.put("success", resultList);
			results.put("error", errorList);
			results.put("exit", session.getExitStatus());
			return results;
		} catch(Exception e) {
			throw e;
		} finally {
			if (stdInput != null) {
				stdInput.close();
			}
			if (stdError != null) {
				stdError.close();
			}
			if(session != null) {
				session.close();
			}
			if(connection != null){
				connection.close();
			}
		}
	}

	@Override
	public void exit() {
		this.exit = true;
	}

}
