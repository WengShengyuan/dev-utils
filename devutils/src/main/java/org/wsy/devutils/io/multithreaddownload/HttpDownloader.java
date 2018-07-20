package org.wsy.devutils.io.multithreaddownload;

import org.wsy.devutils.io.multithreaddownload.state.COMMON_DOWNLOAD_STATE;
import org.wsy.devutils.thread.ConfigurableThread;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Observable;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;

/**
 * 下载器 (观察者模式)
 * @ClassName HttpDownloader
 * @author wengshengyuan
 * @Date 2017年12月4日 下午2:48:13
 * @version 1.0.0
 */
public class HttpDownloader extends Observable {

	private static final Logger logger = LoggerFactory.getLogger(HttpDownloader.class);
	
	String url;
	private File destFilePart;//destFile.part{id}   目标文件块地址文件
	long startIndex;//片段起始位置
	long endIndex;//片段文件终结位置
	int downloaderId; //下载Id
	int limitSpeed; //限速标志(单位Kb/s)
	Semaphore semaphore;
	
	private static final String SUFFIX = ".dltmp"; // 最终为:destFile.part{id}.dltmp
	
	
	
	public String getUrl() {
		return url;
	}

	public File getDestFilePart() {
		return destFilePart;
	}

	public long getStartIndex() {
		return startIndex;
	}

	public long getEndIndex() {
		return endIndex;
	}

	public int getDownloaderId() {
		return downloaderId;
	}
	
	public int getLimitSpeed(){
		return limitSpeed;
	}


	public HttpDownloader(String url, int downloaderId, File destFilePart, long startIndex, long endIndex,int limitSpeed, Semaphore semaphore) {
		this.url = url;
		this.downloaderId = downloaderId;
		this.destFilePart = destFilePart;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.limitSpeed = limitSpeed;
		this.semaphore = semaphore;
	}
	
	private void notifyWorking(){
		try {
			this.semaphore.acquire();
			setChanged();
			notifyObservers(COMMON_DOWNLOAD_STATE.WORKING);
			this.semaphore.release();
		}catch(Exception e) {
			logger.error("获取信号量异常. "+e.getMessage(),e);
		}
	}
	private void notifySuccess(){
		try {
			this.semaphore.acquire();
			setChanged();
			notifyObservers(COMMON_DOWNLOAD_STATE.SUCCESS);
			this.semaphore.release();
		}catch(Exception e) {
			logger.error("获取信号量异常. "+e.getMessage(),e);
		}
	}
	private void notifyFail(){
		try {
			this.semaphore.acquire();
			setChanged();
			notifyObservers(COMMON_DOWNLOAD_STATE.FAIL);
			this.semaphore.release();
		}catch(Exception e) {
			logger.error("获取信号量异常. "+e.getMessage(),e);
		}
	}
	

	public void execute(Executor execute){
		execute.execute(new Worker().enableExeLimit().setExeLimit(5));
	}
	
	/**
	 * 文件片段下载线程
	 * 1、destfile存在，直接success<br>
	 * 2、dltmp文件存在则重新下载该片段<br>
	 * @ClassName Downloader
	 * @author wengshengyuan
	 * @Date 2017年12月4日 上午11:13:34
	 * @version 1.0.0
	 */
	private class Worker extends ConfigurableThread {
		@Override
		protected void doRun(){
			InputStream inputStream=null;
			BufferedOutputStream outputStream=null;
			try {
				URL urlPath = new URL(url);
	    		if(destFilePart.exists()){
	    			logger.info("第{}块文件已下载完毕,无需重新下载...", HttpDownloader.this.downloaderId);
	    			markAsSuccess();
	    		}else{
	    			HttpDownloader.this.notifyWorking();
	    			logger.info("下载线程 {} 开始下载...",HttpDownloader.this.destFilePart.getAbsolutePath());
	    			File tempFile = new File(destFilePart.getAbsolutePath()+SUFFIX);
	    			if(tempFile.exists()){
	    				tempFile.delete();
	    			}
	    			HttpURLConnection connection = (HttpURLConnection) urlPath.openConnection();
	                connection.setRequestMethod("GET");
	                connection.setConnectTimeout(10000);//超时时间
	                //设置分段下载的头信息。  Range:做分段数据请求用的。格式: Range bytes=0-1024  或者 bytes:0-1024
	                connection.setRequestProperty("Range", "bytes="+ startIndex + "-" + endIndex);
	                logger.info("文件块_{}的下载起点是:{}  下载终点是:{}", downloaderId,startIndex,endIndex);
	                if(connection.getResponseCode() == 206){
	                	inputStream = connection.getInputStream();
	                	outputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
	                    // 将网络流中的文件写入本地
	                    byte[] buffer = new byte[1024];
	                    long prevTime = System.currentTimeMillis(); //记录系统时间
	                    int byteSum = 0;//总共读取的文件块大小
	                    int length = -1;
	                    while((length = inputStream.read(buffer)) > 0){
	                    	//---限速start ---
	                    	byteSum += length;
	                        long currentTime = System.currentTimeMillis();//当前时间
	                        int speed = 0;
	                        if (currentTime - prevTime > 0) {//避免两次读数太近，导致分母为0
	                            speed = (int) (byteSum / (currentTime - prevTime));
	                        }
	                        if (limitSpeed > 0 && ((endIndex-startIndex+1) - byteSum) > limitSpeed) {//设置了限速
	                            if (speed > limitSpeed) {
	                                //计算出需要等待多久才能达到限速要求：
	                                int sleepTime = (int) (byteSum / limitSpeed + prevTime - currentTime);
	                                Thread.sleep(sleepTime);
	                            }
	                        }
	                        //---限速end ---
	                        outputStream.write(buffer, 0, length);
	                    }
	                    logger.info("第"+downloaderId+"块下载用时-->"+(System.currentTimeMillis()-prevTime));
	                    outputStream.close();
	                    inputStream.close();
	                    logger.info("文件块{}下载完毕...", downloaderId);
	                    markAsSuccess();
	                } else {
	                	logger.info("下载任务{}下载完毕失败,响应码是{}", HttpDownloader.this.destFilePart.getAbsolutePath(),connection.getResponseCode());
	                }
	    		}
			} catch (Exception e) {
				logger.error("下载任务"+HttpDownloader.this.destFilePart.getAbsolutePath()+"下载完毕失败."+e.getMessage(),e);
			} finally {
				try {
					if(inputStream!= null)
						inputStream.close();
				} catch (IOException e) {
					logger.error("inputStream 释放失败."+e.getMessage(),e);
				}
				try {
					if(outputStream != null)
						outputStream.close();
				} catch (IOException e) {
					logger.error("outputStream 释放失败."+e.getMessage(),e);
				}
			}
			
		}
		
		@Override
		protected void onTimeout() {
			logger.info("下载任务{}执行时间超时，线程结束",HttpDownloader.this.destFilePart.getAbsolutePath());
			File tmpFile = new File(HttpDownloader.this.destFilePart.getAbsolutePath()+SUFFIX);
			if(tmpFile.exists()) {
				tmpFile.delete();
			}
			HttpDownloader.this.notifyFail();
		}

		@Override
		protected void onTryout() {
			logger.info("下载任务{}尝试次数超出，线程结束",HttpDownloader.this.destFilePart.getAbsolutePath());
			File tmpFile = new File(HttpDownloader.this.destFilePart.getAbsolutePath()+SUFFIX);
			if(tmpFile.exists()) {
				tmpFile.delete();
			}
			HttpDownloader.this.notifyFail();
		}

		@Override
		protected void onSuccess() {
			logger.info("下载任务{}执行成功，线程结束",HttpDownloader.this.destFilePart.getAbsolutePath());
			File tmpFile = new File(HttpDownloader.this.destFilePart.getAbsolutePath()+SUFFIX);
			if(tmpFile.exists()) {
				try {
					FileUtils.moveFile(tmpFile, destFilePart);
				} catch (IOException e) {
					threadExceptionHandle(e);
				}
			}
			HttpDownloader.this.notifySuccess();
		}

		@Override
		protected void threadExceptionHandle(Exception e) {
			logger.info("下载任务"+HttpDownloader.this.destFilePart.getAbsolutePath()+"线程执行错误，程序退出.错误信息:" + e);
			File tmpFile = new File(HttpDownloader.this.destFilePart.getAbsolutePath()+SUFFIX);
			if(tmpFile.exists()) {
				tmpFile.delete();
			}
			HttpDownloader.this.notifyFail();
		}
	}
}
