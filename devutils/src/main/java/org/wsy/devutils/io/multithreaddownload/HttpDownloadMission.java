package org.wsy.devutils.io.multithreaddownload;

import com.rails.wifi.utildevutilscollection.io.FileMd5Check;
import com.rails.wifi.utildevutilscollection.io.FilePartProcess;
import com.rails.wifi.utildevutilscollection.io.multithreaddownload.exceptions.MissionInitException;
import com.rails.wifi.utildevutilscollection.io.multithreaddownload.state.COMMON_DOWNLOAD_STATE;
import com.rails.wifi.utildevutilscollection.io.multithreaddownload.state.DownloaderState;
import com.rails.wifi.utildevutilscollection.io.multithreaddownload.state.MissionState;
import com.rails.wifi.utildevutilscollection.io.wrapper.FilePartProperties;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * 下载任务
 * 
 * @ClassName HttpDownloadMission
 * @author wengshengyuan
 * @Date 2017年12月4日 下午1:43:44
 * @version 1.0.0
 */
public class HttpDownloadMission {

	private static final Logger logger = LoggerFactory.getLogger(HttpDownloadMission.class);

	private String url;// 下载地址
	private String missionId;// 任务ID
	private File destFile;// 目的文件
	private File tmpDestFolder;// 保存路径同名目录
	private File missionDescribeFile;// 下载任务临时文件 //保存路径同名目录/文件名.mstmp
	private int fileSize;// 完整文件大小
	private int workersCount; // 线程数
	private int blockCount; // 分割块数
	private String destFileMd5;// 目的文件md5
	private int limitSpeed;
	Map<Integer, HttpDownloader> downLoaderMap;// downloader列表
	MissionState missionState;// 状态描述
	ExecutorService downLoaders;// 下载线程池
	
	Semaphore stateUpdateSemaphore = new Semaphore(1);// 更新状态的信号量,所有Downloader只允许有一个在更新

	private static final int DEFAULT_WORKERS = 5;// 默认最大线程数
	private static final int DEFAULT_BLOCKSIZE = 1024 * 1024; // 默认文件块大小
	private static final String SUFFIX = ".mstmp";// 描述文件后缀
	private static final String UTF8 = "UTF8";

	@Override
	public String toString() {
		return "HttpDownloadMission [url=" + url + ", missionId=" + missionId + ", destFile=" + destFile
				+ ", tmpDestFolder=" + tmpDestFolder + ", missionDescribeFile=" + missionDescribeFile + ", fileSize="
				+ fileSize + ", workersCount=" + workersCount + ", blockCount=" + blockCount + ", destFileMd5="
				+ destFileMd5 + ", downloaders=" + downLoaderMap + ", missionState=" + missionState + ", downLoaders="
				+ downLoaders + "]";
	}

	/**
	 * 构造函数
	 * 
	 * @param url
	 * @param missionId
	 * @param destFile
	 * @param urlPartFileMd5
	 * @throws Exception
	 */
	public HttpDownloadMission(String url, String missionId, File destFile, String urlPartFileMd5, int limitSpeed)
			throws Exception {
		this.url = url;
		this.missionId = missionId;
		this.destFile = destFile;
		this.workersCount = DEFAULT_WORKERS;
		this.destFileMd5 = urlPartFileMd5;
		this.limitSpeed = limitSpeed;
		initMission();
	}

	/**
	 * 构造函数、指定最大线程数量
	 * 
	 * @param url
	 * @param missionId
	 * @param destFile
	 * @param workersCount
	 * @param urlPartFileMd5
	 * @throws Exception
	 */
	public HttpDownloadMission(String url, String missionId, File destFile, int workersCount, String urlPartFileMd5,
			int limitSpeed) throws Exception {
		this.url = url;
		this.missionId = missionId;
		this.destFile = destFile;
		this.workersCount = workersCount;
		this.destFileMd5 = urlPartFileMd5;
		this.limitSpeed = limitSpeed;
		initMission();
	}

	/**
	 * 初始化并创建描述文件
	 * 如果文件存在则跳过，如果文件不存在则创建，如果下载文件已存在则检测是否和服务器一致，若一致则创建一个已成功的描述文件，若不成功，则删除临时文件，
	 * 并重新创建
	 * 
	 * @throws Exception
	 */
	private void initMission() throws Exception {
		missionState = new MissionState();
		downLoaderMap = new HashMap<Integer, HttpDownloader>();
		String destFileName = destFile.getName().substring(0, destFile.getName().lastIndexOf("."));// 不带后缀的文件名
		tmpDestFolder = new File(destFile.getParent() + File.separator + destFileName);// 保存路径同名目录
		missionDescribeFile = new File(tmpDestFolder.getAbsolutePath() + File.separator + destFile.getName() + SUFFIX);// 保存路径同名目录/文件名.mstmp
		getContentLength(this.url);
		logger.info("获取文件大小为{}", this.fileSize);
		blockCount = this.fileSize % DEFAULT_BLOCKSIZE == 0 ? this.fileSize / DEFAULT_BLOCKSIZE
				: this.fileSize / DEFAULT_BLOCKSIZE + 1;
		logger.info("文件查分为{}块", blockCount);
		if (destFile.exists()) {
			if (destFile.length() == this.fileSize && FileMd5Check.getMD5Checksum(destFile).equals(destFileMd5)) {
				logger.info("任务已完成，目的文件已存在，退出初始化过程...");
				deleteTmpFile();
			} else {
				destFile.delete();
				deleteTmpFile();
			}
		}

		if (!missionDescribeFile.exists()) {
			logger.info("该任务无描述文件，创建....{}", missionDescribeFile.getAbsolutePath());
			if (!tmpDestFolder.exists())
				tmpDestFolder.mkdirs();

			missionState = new MissionState(this.blockCount, COMMON_DOWNLOAD_STATE.INITED, this.missionId);
			persistStateLine();
			logger.info("描述文件{}创建成功", missionDescribeFile.getAbsolutePath());
		} else {
			logger.info("任务已有描述文件 {}", missionDescribeFile.getAbsolutePath());
			loadStateLine();
			if (missionState.getBlockCount() != this.blockCount) {
				throw new Exception("block数量不匹配");
			} else {
				logger.info("描述文件校验正常");
			}
			if (!missionState.isCanContinue()) {
				logger.info("上次记录为暂停，重新启动....");
				missionState.setCanContinue(true);
				persistStateLine();
			}
		}
	}

	/**
	 * 开始下载任务
	 * 
	 * @throws Exception
	 */
	public void startMission() throws Exception {
		logger.info("开始下载任务...准备更新任务状态.");
		missionState.setMissionState(COMMON_DOWNLOAD_STATE.WORKING);
		persistStateLine();
		logger.info("任务更新为正在工作，开始初始化线程池...");
		downLoaders = Executors.newFixedThreadPool(this.workersCount); // 创建线程池
		splitDownload();
	}

	public boolean isWorking() {
		return missionState.getMissionState() == COMMON_DOWNLOAD_STATE.WORKING;
	}

	public boolean containsFail() {
		return missionState.getFailCount() > 0;
	}

	public MissionState getMissionState() {
		return missionState;
	}

	public void restartFailBlocks() {
		logger.info("尝试重新开始失败文件块下载...当前包含失败块数:{}", missionState.getFailCount());
		for (DownloaderState state : missionState.getFails()) {
			HttpDownloader downloader = generateDownloader(this.url, state.getDownloaderId(),
					new File(tmpDestFolder.getAbsolutePath() + File.separator + destFile.getName() + ".part_"
							+ state.getDownloaderId()),
					this.limitSpeed);
			downloader.addObserver(new DownloaderObserver());
			downloader.execute(this.downLoaders);
			downLoaderMap.put(state.getDownloaderId(), downloader);
		}
	}

	/**
	 * 标记任务可进行状态为false
	 * 
	 * @throws Exception
	 */
	public void stopMission() throws Exception {
		logger.info("准备停止任务线程,并更新描述文件....");
		downLoaders.shutdownNow();
		missionState.setCanContinue(false);
		persistStateLine();
	}

	private HttpDownloader generateDownloader(String url, int id, File partDestFile, int limitSpeed) {
		int startPos = DEFAULT_BLOCKSIZE * id;
		int endPos = (id == this.blockCount - 1) ? (this.fileSize - 1) : (DEFAULT_BLOCKSIZE * (id + 1) - 1);
		HttpDownloader downloader = new HttpDownloader(this.url, id, partDestFile, startPos, endPos, this.limitSpeed,this.stateUpdateSemaphore);
		DownloaderState newState = new DownloaderState();
		newState.setDownloaderId(id);
		newState.setDownloaderState(COMMON_DOWNLOAD_STATE.INITED);
		missionState.getDownloaderStates().put(id + "", newState);
		return downloader;
	}

	private void splitDownload() throws MissionInitException {
		logger.info("开始创建分块下载线程...");
		try {
			for (int i = 0; i < this.blockCount; i++) {
				if (this.missionState.isCanContinue()) {
					File partFile = new File(
							tmpDestFolder.getAbsolutePath() + File.separator + destFile.getName() + ".part_" + i);
					logger.info("创建下载线程....id={}", i);
					HttpDownloader downloader = generateDownloader(url, i, partFile, this.limitSpeed);
					downloader.addObserver(new DownloaderObserver());
					downloader.execute(this.downLoaders);
					downLoaderMap.put(i, downloader);
				} else {
					logger.info("任务canContinue为false，不继续生成downloader");
				}
			}
			if (!this.missionState.isCanContinue()) {
				downLoaders.shutdownNow();
			}
		} catch (Exception e) {
			logger.info("文件拆分异常." + e.getMessage(), e);
			throw new MissionInitException("mission split error.", e.getCause());
		}
	}

	/**
	 * 删除临时文件夹和描述文件
	 */
	public void deleteTmpFile() {
		logger.info("开始删除临时文件...");
		try {
			if (missionDescribeFile.exists()) {
				missionDescribeFile.delete();
			}
			if (tmpDestFolder.exists()) {
				FileUtils.deleteDirectory(tmpDestFolder);
			}
			logger.info("临时文件删除完毕..");
		} catch (IOException e) {
			logger.error("destroyMission 异常" + e.getMessage(), e);
		}
	}

	/**
	 * 更新下载线程状态并保存在文件中
	 * 
	 * @param workerId
	 * @param state
	 * @throws Exception
	 */
	private void updateWorkerState(int workerId, int state) throws Exception {
		missionState.updateDownloaderState(workerId, state);
		persistStateLine();
	}

	/**
	 * 将状态写入文件
	 * 
	 * @throws Exception
	 */
	private void persistStateLine() throws Exception {
		FileUtils.writeStringToFile(missionDescribeFile, missionState.toStateLine(), UTF8);
	}

	/**
	 * 将状态读取进内存
	 * 
	 * @throws Exception
	 */
	private void loadStateLine() throws Exception {
		String line = FileUtils.readFileToString(missionDescribeFile, UTF8);
		missionState = new MissionState(line);
	}

	/**
	 * 更新mstmp文件，线程安全
	 * 
	 * @param workerId
	 * @param status
	 */
	private void update(int workerId, int status) throws Exception {
		logger.info("downloader {} 上报状态 {} ...", workerId, status);
		updateWorkerState(workerId, status);

		if (status == COMMON_DOWNLOAD_STATE.SUCCESS || status == COMMON_DOWNLOAD_STATE.FAIL) {
			downLoaderMap.get(workerId).deleteObservers();
			downLoaderMap.remove(workerId);
			logger.info("踢出不工作worker: {}, map中存留worker: {}", workerId, downLoaderMap.keySet().size());
		}

		if (missionState.isAllFinished() || downLoaderMap.keySet().size() < 1) {
			// 全部完成
			if (missionState.isAllSuccess()) {
				logger.info("downloader任务全部完成. 全部文件块下载成功,准备合并文件....");
				mergeDownload();
				this.missionState.setMissionState(COMMON_DOWNLOAD_STATE.SUCCESS);
				downLoaders.shutdownNow();
			} else {
				logger.warn("downloader任务全部完成. 包含失败文件块....");
				this.missionState.setMissionState(COMMON_DOWNLOAD_STATE.FAIL);
				downLoaders.shutdownNow();
			}
		} else if (missionState.getFailCount() > 0) {
			// 有异常
			logger.warn("包含失败文件块数量: {}", missionState.getFailCount());
		} else {
			// 工作中
		}
	}

	private synchronized void mergeDownload() throws Exception {
		List<File> parts = Arrays.asList(tmpDestFolder.listFiles()).stream()
				.filter(part -> part.getName().contains(".part_")).collect(Collectors.toList());

		logger.info("整合 {} 个子文件   -> {}... ", parts.size(), destFile.getAbsoluteFile());
		if (parts.size() != (this.blockCount))
			throw new Exception("文件块数目与分块数目不符.");

		logger.info("准备合并文件...");
		List<FilePartProperties> properties = new ArrayList<FilePartProperties>();
		for (int i = 0; i < parts.size(); i++) {
			File part = new File(tmpDestFolder.getAbsolutePath() + File.separator + destFile.getName() + ".part_" + i);
			if (part.exists()) {
				FilePartProperties partProperties = new FilePartProperties();
				partProperties.setFullLength(this.fileSize);
				partProperties.setFullMd5(this.destFileMd5);
				partProperties.setPartOrder(i);
				partProperties.setPartFilePath(part.getAbsolutePath());
				partProperties.setPartLength(part.length());
				partProperties.setPartFileMd5(FileMd5Check.getMD5Checksum(part));
				properties.add(partProperties);
			} else {
				throw new FileNotFoundException("part file " + part.getAbsolutePath() + " not found");
			}
		}

		destFile = FilePartProcess.merge(properties, destFile);

		logger.info("子文件整合完成,开始注销下载任务... ");
		deleteTmpFile();
	}

	class DownloaderObserver implements Observer {

		@Override
		public void update(Observable o, Object arg) {
			try {
				HttpDownloadMission.this.update(((HttpDownloader) o).getDownloaderId(), (Integer) arg);
			} catch (Exception e) {
				logger.error("更新状态异常。" + e.getMessage(), e);
			}
		}

	}

	private void getContentLength(String fileUrl) throws IOException {
		logger.info("获取目标文件{}大小...", fileUrl);
		URL url = new URL(fileUrl);
		URLConnection connection = url.openConnection();
		this.fileSize = connection.getContentLength();
	}
}
