package com.emc.mongoose.core.impl.persist;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
//
import com.emc.mongoose.core.api.load.model.Consumer;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.persist.DataItemBuffer;
//
import com.emc.mongoose.core.impl.load.executor.util.ConsumerBase;
//
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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
/**
 Created by andrey on 28.09.14.
 */
public class TmpFileItemBuffer<T extends DataItem>
extends ConsumerBase<T>
implements DataItemBuffer<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final File fBuff;
	private final AtomicLong writtenDataItems = new AtomicLong(0);
	private volatile ObjectOutput fBuffOut;
	//
	public TmpFileItemBuffer(final long maxCount)
	throws IllegalStateException {
		super(RunTimeConfig.getContext(), maxCount);
		//
		try {
			fBuff = Files.createTempFile(
				String.format(FMT_THREAD_NAME, RunTimeConfig.getContext().getRunName()), null
			).toFile();
			fBuff.deleteOnExit();
		} catch(final IOException e) {
			throw new IllegalStateException("Failed to create temporary file for output", e);
		}
		LOG.debug(LogUtil.MSG, "{}: created temp file", toString());
		// the name should be URL-safe
		setName(fBuff.getName());
		try {
			fBuffOut = new ObjectOutputStream(
				new FileOutputStream(fBuff)
			);
		} catch(final IOException e) {
			throw new IllegalStateException("Failed to open temporary file for output", e);
		}
	}
	//
	@Override
	public final String toString() {
		return getName();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Consumer implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void submitSync(T dataItem)
	throws RejectedExecutionException {
		if(fBuffOut == null) {
			throw new RejectedExecutionException();
		} else if(dataItem == null) {
			shutdown();
		} else {
			try {
				fBuffOut.writeObject(dataItem);
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to serialize the data item");
			}
		}
	}
	//
	@Override
	public final synchronized void close()
	throws IOException {
		if(fBuffOut != null) {
			shutdown();
			try {
				while(isAlive()) {
					TimeUnit.MILLISECONDS.sleep(submTimeOutMilliSec);
				}
			} catch(final InterruptedException e) {
				LOG.debug(LogUtil.ERR, "Interrupted while serializing the remaining data items");
			} finally {
				LOG.debug(
					LogUtil.MSG, "{}: output done, {} items", toString(), writtenDataItems.get()
				);
				super.close();
				fBuffOut.close();
				fBuffOut = null;
			}
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Producer implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private volatile Consumer<T> consumer = null;
	//
	private final Thread producerThread = new Thread("producer<" + getName() + ">") {
		//
		{ setDaemon(true); } // do not block process exit
		//
		@Override @SuppressWarnings("unchecked")
		public final void run() {
			if(fBuffOut == null) {
				LOG.debug(LogUtil.MSG, "{}: producing started", getName());
				//
				final long maxProduceCount;
				long consumerCountLimit = Long.MAX_VALUE;
				try {
					consumerCountLimit = consumer.getMaxCount();
				} catch(final RemoteException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Looks like network failure");
				}
				maxProduceCount = Math.min(writtenDataItems.get(), consumerCountLimit);
				LOG.debug(
					LogUtil.MSG, "{}: produce count limit {}", getName(), maxProduceCount
				);
				//
				if(fBuff == null) {
					LOG.warn(LogUtil.ERR, "No temporary file is available for producing");
				} else {
					T nextDataItem;
					try(
						final ObjectInput
							fBuffIn = new ObjectInputStream(new FileInputStream(fBuff))
					) {
						for(long i = 0; i < maxProduceCount; i ++) {
							nextDataItem = (T) fBuffIn.readObject();
							if(nextDataItem == null) {
								break;
							}
							consumer.submit(nextDataItem);
						}
						LOG.debug(LogUtil.MSG, "done producing");
					} catch(final RemoteException e) {
						LogUtil.exception(LOG, Level.DEBUG, e, "Failed to submit a data item");
					} catch(final IOException | ClassNotFoundException | ClassCastException e) {
						LogUtil.exception(LOG, Level.WARN, e, "Failed to read a data item");
					} catch(final RejectedExecutionException e) {
						LOG.debug(LogUtil.ERR, "Consumer rejected the data item");
					} catch(final InterruptedException e) {
						LOG.debug(LogUtil.MSG, "Interrupted");
					} finally {
						try {
							consumer.shutdown();
						} catch(final RemoteException e) {
							LogUtil.exception(LOG, Level.WARN, e, "Looks like network failure");
						} finally {
							consumer = null;
							deleteFromFileSystem();
						}
					}
				}
			} else {
				LOG.warn(
					LogUtil.ERR,
					"Failed to start \"{}\" producing: illegal state: output isn't closed yet",
					getName()
				);
			}
		}
	};
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
	public final void start() {
		producerThread.start();
	}
	//
	@Override
	public final void await()
	throws InterruptedException {
		producerThread.join();
	}
	//
	@Override
	public final void await(final long timeOut, final TimeUnit timeUnit)
	throws InterruptedException {
		timeUnit.timedJoin(this, timeOut);
	}
	//
	@Override
	public final synchronized void interrupt() {
		if(consumer != null) {
			try {
				consumer.shutdown();
			} catch(final RemoteException | RejectedExecutionException ignored) {
			}
		}
		producerThread.interrupt();
		deleteFromFileSystem();
		LOG.debug(LogUtil.MSG, "{}: interrupted", getName());
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	private void deleteFromFileSystem() {
		if(fBuff.exists()) {
			if(fBuff.delete()) {
				LOG.debug(
					LogUtil.MSG, "File \"{}\" succesfully deleted",
					fBuff.getAbsolutePath()
				);
			} else {
				LOG.debug(
					LogUtil.ERR, "Failed to delete the file \"{}\"",
					fBuff.getAbsolutePath()
				);
			}
		}
	}
}
