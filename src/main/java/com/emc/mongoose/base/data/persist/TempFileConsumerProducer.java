package com.emc.mongoose.base.data.persist;
//
import com.emc.mongoose.base.load.Consumer;
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.base.load.Producer;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;
//
import com.emc.mongoose.util.logging.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
/**
 Created by andrey on 28.09.14.
 */
public class TempFileConsumerProducer<T extends DataItem>
extends Thread
implements Consumer<T>, Producer<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final File fBuff;
	private final ObjectOutput fBuffOut;
	private final ExecutorService outPutExecutor;
	private volatile long maxCount;
	private final AtomicLong writtenDataItems = new AtomicLong(0);
	private final RunTimeConfig runTimeConfig;
	private final int retryCountMax, retryDelayMilliSec;
	//
	public TempFileConsumerProducer(
		final RunTimeConfig runTimeConfig,
		final String prefix, final String suffix, final int threadCount, final long maxCount
	) {
		//
		this.runTimeConfig = runTimeConfig;
		retryCountMax = runTimeConfig.getRunRetryCountMax();
		retryDelayMilliSec = runTimeConfig.getRunRetryDelayMilliSec();
		//
		File fBuffTmp = null;
		try {
			fBuffTmp = Files
				.createTempFile(prefix, suffix)
				.toFile();
		} catch(final IllegalArgumentException | UnsupportedOperationException | IOException | SecurityException  e) {
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to create temp file");
		} finally {
			fBuff = fBuffTmp;
		}
		//
		ObjectOutput fBuffOutTmp = null;
		try {
			assert fBuff!=null;
			fBuffOutTmp = new ObjectOutputStream(
				new FileOutputStream(fBuff)
			);
		} catch(final IOException e) {
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to open temporary file for output");
		} finally {
			fBuffOut = fBuffOutTmp;
		}
		//
		outPutExecutor = Executors.newFixedThreadPool(threadCount);
		this.maxCount = maxCount;
		//
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Consumer implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static class DataItemOutPutTask<T>
	implements Runnable {
		//
		private final ObjectOutput fBuffOut;
		private final T dataItem;
		//
		private DataItemOutPutTask(final ObjectOutput fBuffOut, final T dataItem) {
			this.fBuffOut = fBuffOut;
			this.dataItem = dataItem;
		}
		//
		@Override
		public final void run() {
			try {
				fBuffOut.writeObject(dataItem);
			} catch(final IOException e) {
				ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to write out the data item");
			}
		}
	}
	//
	@Override
	public final void submit(T dataItem)
	throws IllegalStateException {
		//
		if(outPutExecutor.isShutdown() || outPutExecutor.isTerminated()) {
			throw new IllegalStateException("Output has been already closed");
		}
		//
		final DataItemOutPutTask<T> outPutTask = new DataItemOutPutTask<>(fBuffOut, dataItem);
		boolean passed = false;
		int rejectCount = 0;
		do {
			try {
				outPutExecutor.submit(outPutTask);
				passed = true;
				writtenDataItems.incrementAndGet();
			} catch(final RejectedExecutionException e) {
				rejectCount ++;
				try {
					Thread.sleep(rejectCount * retryDelayMilliSec);
				} catch(final InterruptedException ee) {
					break;
				}
			}
		} while(!passed && rejectCount < retryCountMax && writtenDataItems.get() < maxCount);
		//
		if(!passed) {
			LOG.debug(
				Markers.ERR, "Data item \"{}\" has been rejected after {} tries",
				dataItem, rejectCount
			);
		}
	}
	//
	@Override
	public final long getMaxCount() {
		return maxCount;
	}
	@Override
	public final void setMaxCount(final long maxCount) {
		this.maxCount = maxCount;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Closeable implementation ////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void close()
	throws IOException {
		//
		LOG.trace(Markers.MSG, "Closing the output");
		//
		outPutExecutor.shutdown();
		try {
			outPutExecutor.awaitTermination(
				runTimeConfig.getRunReqTimeOutMilliSec(), TimeUnit.MILLISECONDS
			);
		} catch(final InterruptedException e) {
			ExceptionHandler.trace(
				LOG, Level.DEBUG, e, "Interrupted while writing out the remaining data items"
			);
		} finally {
			final int droppedTaskCount = outPutExecutor.shutdownNow().size();
			LOG.debug(
				Markers.MSG, "Wrote {} data items, dropped {}",
				writtenDataItems.addAndGet(-droppedTaskCount), droppedTaskCount
			);
		}
		//
		fBuffOut.close();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Producer implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private volatile Consumer<T> consumer = null;
	//
	@Override
	public final void setConsumer(Consumer<T> consumer) {
		this.consumer = consumer;
	}
	//
	@Override
	public final Consumer<T> getConsumer() {
		return consumer;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final void run() {
		//
		if(!outPutExecutor.isTerminated()) {
			throw new IllegalStateException("Output has not been closed yet");
		}
		//
		long
			availDataItems = writtenDataItems.get(),
			consumerMaxCount = Long.MAX_VALUE;
		try {
			consumerMaxCount = consumer.getMaxCount();
		} catch(final RemoteException e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "Looks like network failure");
		}
		//
		T nextDataItem;
		try(final ObjectInput fBuffIn = new ObjectInputStream(new FileInputStream(fBuff))) {
			while(availDataItems --> 0 && consumerMaxCount --> 0) {
				nextDataItem = (T) fBuffIn.readObject();
				consumer.submit(nextDataItem);
				if(nextDataItem == null) {
					break;
				}
			}
		} catch(final IOException | ClassNotFoundException | ClassCastException e) {
			ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to read a data item");
		} finally {
			try {
				consumer.setMaxCount(writtenDataItems.get());
			} catch(final RemoteException e) {
				ExceptionHandler.trace(LOG, Level.WARN, e, "Looks like network failure");
			}
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
}
