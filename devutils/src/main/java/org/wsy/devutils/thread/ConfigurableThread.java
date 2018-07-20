package org.wsy.devutils.thread;

import java.util.Date;

/**
 * 可定制的线程子类
 * @ClassName ConfigurableThread
 * @Description 支持执行时长限制/重试次数限制，支持事件行为重写
 * @author wengshengyuan
 * @Date 2017年9月20日 上午8:38:04
 * @version 1.0.0
 */
public class ConfigurableThread extends Thread {
	
	private boolean firstExe = false;
	private boolean enableTimeLimit = false;
	private boolean enableExeLimit = false;
	private Date firstExeTime;
	private Integer exeLimit = 0;
	private Long timeLimit = 0L;
	private Long increaseStep = 0L;
	
	protected Long timeInterval = 1000L;
	protected Integer tryCount = 1;
	protected boolean successFlag = false; 
	
	/**
	 * 执行逻辑主体
	 */
	@Override
	public void run(){
		if(!firstExe){
			this.firstExe = true;
			this.firstExeTime = new Date();
		}
		try {
			while(execuble()){
				doRun();
				tryCount ++;
				if(!successFlag) {
					Thread.sleep(this.timeInterval);
				}
				this.timeInterval += this.increaseStep;
			}
			return;
		} catch (Exception e) {
			threadExceptionHandle(e);
		}
	}
	
	/**
	 * 子类必须重写该方法
	 * @Description 具体业务相关的行为放在这里
	 * @throws Exception
	 */
	protected void doRun() throws Exception{
		throw new Exception("还未声明实现方法");
	}
	
	/**
	 * 超时处理
	 */
	protected void onTimeout() {
		System.out.println("执行时间超时，线程结束");
	}
	
	/**
	 * 重试次数上限处理
	 */
	protected void onTryout() {
		System.out.println("尝试次数超出，线程结束");
	}
	
	/**
	 * 成功处理
	 */
	protected void onSuccess() {
		System.out.println("任务执行成功，线程结束");
	}
	
	/**
	 * run方法的异常处理
	 */
	protected void threadExceptionHandle(Exception e) {
		System.out.println("线程执行错误，程序退出.错误信息:"+e);
	}
	
	private boolean execuble(){
		if(successFlag){
			onSuccess();
			return false;
		}
		
		if(enableTimeLimit && 
				(System.currentTimeMillis() - this.firstExeTime.getTime()) > this.timeLimit){
			onTimeout();
			return false;
		}
		
		if(enableExeLimit && 
				this.tryCount > this.exeLimit){
			onTryout();
			return false;
		}
		return true;
	}
	
	protected final void markAsSuccess(){
		this.successFlag = true;
	}
	
	public final ConfigurableThread enableTimeLimit() {
		this.enableTimeLimit = true;
		return this;
	}
	
	public final ConfigurableThread disableTimeLimit() {
		this.enableTimeLimit = false;
		return this;
	}
	
	public final ConfigurableThread enableExeLimit() {
		this.enableExeLimit = true;
		return this;
	}
	
	public final ConfigurableThread disableExeLimit() {
		this.enableExeLimit = false;
		return this;
	}

	public final ConfigurableThread setExeLimit(Integer exeLimit) {
		this.exeLimit = exeLimit;
		return this;
	}


	public final ConfigurableThread setTimeLimit(Long timeLimit) {
		this.timeLimit = timeLimit;
		return this;
	}
	
	public final ConfigurableThread setInterval(Long interval) {
		this.timeInterval = interval;
		return this;
	}
	
	public final ConfigurableThread setIncreaseStep(Long step) {
		this.increaseStep = step;
		return this;
	}

}
