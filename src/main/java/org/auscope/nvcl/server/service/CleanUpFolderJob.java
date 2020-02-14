package org.auscope.nvcl.server.service;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class CleanUpFolderJob {

	private static final Logger logger = LogManager.getLogger(CleanUpFolderJob.class);

	private long days;
	private String datafolderpath;

    @Value("${data.path}")
	public void setDatapath(String datafolderpath) {
		this.datafolderpath = datafolderpath;
	}
	
    @Value("${msgTimetoLiveDays}") 
	public void setDays(long days) {
		this.days = days;
	}

 	@Scheduled(cron="30 * * * * *")
	protected void executeInternal() throws JobExecutionException {
		int dataFoldersCleaned = 0;
		logger.debug("Data Folder cleaner running");
		File dataFolder = new File(this.datafolderpath);
		if (dataFolder.exists()) {
			for (File CachelistFile : dataFolder.listFiles()) {
				if (CachelistFile.isDirectory()
						&& (CachelistFile.lastModified() < System.currentTimeMillis() - (days * 86400000))) {
					try {
						FileUtils.deleteDirectory(CachelistFile);
						dataFoldersCleaned++;
					} catch (IOException e) {
						// attempt to clean up but ignore if it fails.
						logger.debug("cleanup cache folder " + CachelistFile + " failed");
					}
				}
			}
		}
		logger.debug("Cache Folder cleaner complete, " + dataFoldersCleaned + " folder(s) deleted.");
	}

}