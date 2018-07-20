package org.wsy.devutils.system;

import java.util.Map;

/**
 * 命令行执行工具接口
 * @ClassName CommandExecutor
 * @author wengshengyuan
 * @Date 2017年10月10日 上午8:00:11
 * @version 1.0.0
 */
public interface CommandExecutor {
	
	/**
	 * 设置字符串编码
	 * @param charSet
	 * @return
	 */
	CommandExecutor setCharset(String charSet);
	
	/**
	 * 命令执行
	 * @param cmd 命令行
	 * @param args 参数
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> exe(String cmd, String... args) throws Exception;
	
	/**
	 * 退出执行
	 * @Description 执行后，打上退出标记
	 */
	void exit();
	
}
