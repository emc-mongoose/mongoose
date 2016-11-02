package com.emc.mongoose.storage.mock;

import com.emc.mongoose.common.concurrent.Daemon;
import com.emc.mongoose.storage.mock.impl.http.StorageMockFactory;
import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.ui.config.Config;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig.JobConfig;
import com.emc.mongoose.ui.config.reader.jackson.ConfigParser;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import static com.emc.mongoose.common.Constants.KEY_JOB_NAME;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.IOException;
/**
 Created on 12.07.16.
 */
public class Main {

	static {
		LogUtil.init();
	}

	public static void main(final String[] args)
	throws IOException {
		
		final Config config = ConfigParser.loadDefaultConfig();
		if(config == null) {
			throw new IllegalStateException();
		}
		config.apply(CliArgParser.parseArgs(args));

		final LoadConfig loadConfig = config.getLoadConfig();
		final JobConfig jobConfig = loadConfig.getJobConfig();
		String jobName = jobConfig.getName();
		if(jobName == null) {
			jobName = ThreadContext.get(KEY_JOB_NAME);
			jobConfig.setName(jobName);
		} else {
			ThreadContext.put(KEY_JOB_NAME, jobName);
		}
		if(jobName == null) {
			throw new IllegalStateException("Load job name is not set");
		}
		
		final Logger log = LogManager.getLogger();
		log.info(Markers.MSG, "Configuration loaded");
		
		final StorageConfig storageConfig = config.getStorageConfig();
		final ItemConfig itemConfig = config.getItemConfig();
		final StorageMockFactory storageMockFactory = new StorageMockFactory(
			storageConfig, loadConfig, itemConfig
		);
		if(storageConfig.getMockConfig().getNode()) {
			try(final Daemon storageNodeMock = storageMockFactory.newStorageNodeMock()) {
				storageNodeMock.start();
				try {
					storageNodeMock.await();
				} catch(final InterruptedException ignored) {
				}
			} catch(final Exception e) {
				LogUtil.exception(log, Level.ERROR, e, "Failed to run storage node mock");
			}
		} else {
			try(final Daemon storageMock = storageMockFactory.newStorageMock()) {
				storageMock.start();
				try {
					storageMock.await();
				} catch(final InterruptedException ignored) {
				}
			} catch(final Exception e) {
				LogUtil.exception(log, Level.ERROR, e, "Failed to run storage mock");
			}
		}
	}

}
