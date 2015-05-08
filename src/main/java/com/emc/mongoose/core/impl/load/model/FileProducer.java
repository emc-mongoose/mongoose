package com.emc.mongoose.core.impl.load.model;
//mongoose-common.jar
import com.emc.mongoose.common.concurrent.NamingWorkerFactory;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
//mongoose-core-api.jar
import com.emc.mongoose.core.api.load.model.Consumer;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.model.Producer;
//mongoose-core-impl.jar
import com.emc.mongoose.core.impl.load.model.reader.FileReader;
import com.emc.mongoose.core.impl.load.model.reader.RandomFileReader;
import com.emc.mongoose.core.impl.load.model.reader.SimpleFileReader;
import com.emc.mongoose.core.impl.load.tasks.SubmitTask;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 12.05.14.
 A data item producer which constructs data items while reading the special input file.
 */
public class FileProducer<T extends DataItem>
extends Thread
implements Producer<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final Path fPath;
	private final Constructor<T> dataItemConstructor;
	private final long maxCount;
	private final ExecutorService producerExecSvc;
	//
	private Consumer<T> consumer = null;
	//
	@SuppressWarnings("unchecked")
	public FileProducer(final long maxCount, final String fPathStr, final Class<T> dataItemsImplCls)
	throws NoSuchMethodException, IOException {
		super(fPathStr);
		producerExecSvc = Executors.newFixedThreadPool(
			Producer.WORKER_COUNT, new NamingWorkerFactory(fPathStr)
		);
		fPath = FileSystems.getDefault().getPath(fPathStr);
		if(!Files.exists(fPath)) {
			throw new IOException("File \""+fPathStr+"\" doesn't exist");
		}
		if(!Files.isReadable(fPath)) {
			throw new IOException("File \""+fPathStr+"\" is not readable");
		}
		dataItemConstructor = dataItemsImplCls.getConstructor(String.class);
		//
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
	}
	//
	public final String getPath() {
		return fPath.toString();
	}
	//
	@Override
	public final void run() {
		long dataItemsCount = 0;
		try {
			FileReader reader;
			String nextDataString;
			T nextData;
			LOG.debug(
				LogUtil.MSG, "Going to produce up to {} data items for consumer \"{}\"",
				consumer.getMaxCount(), consumer.toString()
			);
			//
			if (RunTimeConfig.getContext().isEnabledDataRandom()) {
				reader = new RandomFileReader(fPath);
			} else {
				reader = new SimpleFileReader(fPath);
			}
			//
			do {
				if ((nextDataString = reader.getDataItemString()) == null){
					break;
				}
				nextData = dataItemConstructor.newInstance(nextDataString);
				//
				try {
					producerExecSvc.submit(
						SubmitTask.getInstance(consumer, nextData)
					);
					dataItemsCount++;
				} catch (final Exception e) {
					if (
						consumer.getMaxCount() > dataItemsCount &&
							!RejectedExecutionException.class.isInstance(e)
						) {
						LogUtil.failure(LOG, Level.WARN, e, "Failed to submit data item");
						break;
					} else {
						LogUtil.failure(LOG, Level.DEBUG, e, "Failed to submit data item");
					}
				}
			} while (!isInterrupted() && dataItemsCount < maxCount);
		} catch(final IOException e) {
			LogUtil.failure(LOG, Level.ERROR, e, "Failed to read line from the file");
		} catch(final Exception e) {
			LogUtil.failure(LOG, Level.ERROR, e, "Unexpected failure");
		} finally {
			LOG.debug(LogUtil.MSG, "Produced {} data items", dataItemsCount);
			try {
				LOG.debug(LogUtil.MSG, "Feeding poison to consumer \"{}\"", consumer.toString());
				if(isInterrupted()) {
					producerExecSvc.shutdownNow();
				} else {
					producerExecSvc.shutdown();
					producerExecSvc.awaitTermination(
						RunTimeConfig.getContext().getRunReqTimeOutMilliSec(),
						TimeUnit.MILLISECONDS
					);
				}
				consumer.shutdown();
			} catch(final Exception e) {
				LogUtil.failure(LOG, Level.WARN, e, "Failed to shut down the consumer");
			}
			LOG.debug(LogUtil.MSG, "Exiting");
		}
	}
	//
	@Override
	public final void setConsumer(final Consumer<T> consumer) {
		LOG.debug(LogUtil.MSG, "Set consumer to \"{}\"", consumer.toString());
		this.consumer = consumer;
	}
	//
	@Override
	public final Consumer<T> getConsumer()
	throws RemoteException {
		return consumer;
	}
	//
	@Override
	public final void interrupt() {
		producerExecSvc.shutdownNow();
	}
}
