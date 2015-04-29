package com.emc.mongoose.persist;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by olga on 29.04.15.
 */
public class Persister {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public Persister(){
		PersistProducer producer;
		PersistConsumer consumer;
		final File folder = new File(Paths.get(LogUtil.PATH_LOG_DIR).toUri());
		try {
			for (final File fileEntry : folder.listFiles()) {
				if (fileEntry.isDirectory()) {
					RunHolder runHolder = new RunHolder(fileEntry.getName());
					consumer = new PersistConsumer(runHolder);
					consumer.run();
					producer = new PersistProducer(runHolder);
					producer.run();
				}
			}
		}catch (final NullPointerException e){
			LogUtil.failure(
				LOG, Level.ERROR, e,
				String.format("There aren't any files in directory: %s",folder.getPath())
			);
		}
	}

}
