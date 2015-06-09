package com.emc.mongoose.core.impl.load.model;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.load.model.AsyncConsumer;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
//
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;
/**
 Created by kurila on 26.05.15.
 */
public abstract class AsyncConsumerBase<T extends DataItem>
extends Thread
implements AsyncConsumer<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static boolean COMPRESSION_ENABLED = false;
	// configuration params
	protected final RunTimeConfig runTimeConfig;
	private final long maxCount;
	protected final int submTimeOutMilliSec, maxQueueSize;
	// states
	private final AtomicLong counterPreSubm = new AtomicLong(0);
	protected final AtomicBoolean
		isStarted = new AtomicBoolean(false),
		isShutdown = new AtomicBoolean(false),
		isAllSubm = new AtomicBoolean(false);
	// volatile
	private final BlockingQueue<T> volatileQueue;
	// persistent
	protected final Class<T> dataCls;
	private final AsyncConsumerBase<T> tmpFileConsumer;
	private volatile FileProducer<T> tmpFileProducer = null;
	//
	public AsyncConsumerBase(
		final Class<T> dataCls, final RunTimeConfig runTimeConfig, final long maxCount
	) {
		this(dataCls, runTimeConfig, maxCount, false);
	}
	//
	public AsyncConsumerBase(
		final Class<T> dataCls, final RunTimeConfig runTimeConfig, final long maxCount,
		final boolean nested
	) throws IllegalStateException {
		this.dataCls = dataCls;
		this.runTimeConfig = runTimeConfig;
		this.maxCount = maxCount > 0 ? maxCount : Long.MAX_VALUE;
		maxQueueSize = (int) Math.min(
			this.maxCount, runTimeConfig.getRunRequestQueueSize()
		);
		volatileQueue = new ArrayBlockingQueue<>(maxQueueSize);
		submTimeOutMilliSec = runTimeConfig.getRunSubmitTimeOutMilliSec();
		//
		if(nested) {
			tmpFileConsumer = null;
		} else {
			final Path tmpFilePath = Paths.get(
				System.getProperty("java.io.tmpdir"),
				runTimeConfig.getRunName() + "-v" + runTimeConfig.getRunVersion()
			);
			if(!tmpFilePath.toFile().exists() && !tmpFilePath.toFile().mkdirs()) {
				LOG.warn(LogUtil.ERR, "Failed to create the directory: \"{}\"", tmpFilePath);
			}
			//
			tmpFileConsumer = new AsyncConsumerBase<T>(
				dataCls, runTimeConfig, maxCount, true
			) {
				//
				private final BufferedWriter tmpFileWriter;
				private final File tmpFile;
				//
				{
					try {
						tmpFile = Files.createTempFile(
							tmpFilePath, runTimeConfig.getRunId(),
							COMPRESSION_ENABLED ? ".gz" : null
						).toFile();
						tmpFile.deleteOnExit();
					} catch(final IOException e) {
						throw new IllegalStateException(
							"Failed to create the temporary file in " + tmpFilePath.toAbsolutePath(), e
						);
					}
					//
					try {
						tmpFileWriter = new BufferedWriter(
							new OutputStreamWriter(
								COMPRESSION_ENABLED ? new GZIPOutputStream(
									Files.newOutputStream(tmpFile.toPath())
								) : Files.newOutputStream(tmpFile.toPath())
							)
						);
					} catch(final IOException e) {
						throw new IllegalStateException(
							"Failed to open the temporary file in " + tmpFilePath.toAbsolutePath(),
							e
						);
					}
					//
					try {
						tmpFileProducer = new FileProducer<>(
							maxCount, tmpFile.getAbsolutePath(), dataCls,
							/*nested=*/true, COMPRESSION_ENABLED
						);
					} catch(final IOException | NoSuchMethodException e) {
						throw new IllegalStateException(e);
					}
					//
					setName("consumer<" + tmpFile.getName() + ">");
				}
				//
				@Override
				protected final void submitSync(final T dataItem)
				throws RejectedExecutionException {
					if(dataItem != null) {
						try {
							synchronized(tmpFileWriter) {
								// TODO SerializationUtils.serialize(dataItem)
								tmpFileWriter.write(dataItem.toString());
								tmpFileWriter.newLine();
							}
						} catch(final IOException e) {
							throw new RejectedExecutionException(e);
						}
					}
				}
				//
				@Override
				public final void interrupt() {
					// the synchronization is necessary here to make sure that every data item is
					// written completely to the file
					synchronized(tmpFileWriter) {
						super.interrupt(); // suspect bug: issue #395
						// should invoke Thread.interrupt() instead this
						// but invokes AsyncConsumerBase.interrupt() actually
					}
					//
					try {
						close();
					} catch(final IOException e) {
						LogUtil.exception(LOG, Level.WARN, e, "Unexpected failure");
					}
					//
					if(tmpFile.delete()) {
						LOG.debug(
							LogUtil.MSG, "{}: temporary file \"{}\" deleted", getName(),
							tmpFile.getAbsolutePath()
						);
					}
				}
				//
				@Override
				public final void close()
				throws IOException {
					try {
						super.close();
					} finally {
						synchronized(tmpFileWriter) {
							tmpFileWriter.close();
						}
						LOG.debug(
							LogUtil.MSG, "{}: closed the file \"{}\" for writing", getName(),
							tmpFile.getAbsolutePath()
						);
					}
				}
			};
			//
			tmpFileConsumer.start();
		}
	}
	//
	@Override
	public void start() {
		if(isStarted.compareAndSet(false, true)) {
			LOG.debug(
				LogUtil.MSG,
				"{}: started, the further consuming will go through the volatile queue",
				getName()
			);
			if(tmpFileConsumer != null && tmpFileConsumer.counterPreSubm.get() > 0) {
				//
				try {
					tmpFileConsumer.close();
				} catch(final IOException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Failed to close the tmp file consumer");
				}
				// means that this is not nested consumer and there are items persisted
				if(tmpFileProducer != null) { // additional check
					tmpFileProducer.setDaemon(true); // do not block process exit
					tmpFileProducer.setConsumer(this); // go through the volatile queue
					tmpFileProducer.start(); // start producing
					LOG.debug(
						LogUtil.MSG, "{}: started producing from file \"{}\"", getName(),
						tmpFileProducer.getPath()
					);
				}
			}
			//
			super.start();
		}
	}
	/**
	 May block the executing thread until the queue becomes able to ingest more
	 @param dataItem
	 @throws RemoteException
	 @throws InterruptedException
	 @throws RejectedExecutionException
	 */
	@Override
	public void submit(final T dataItem)
	throws RemoteException, InterruptedException, RejectedExecutionException {
		if(isStarted.get()) {
			if(dataItem == null || counterPreSubm.get() >= maxCount) {
				shutdown();
			}
			if(isShutdown.get()) {
				throw new InterruptedException("Shut down already");
			}
			if(volatileQueue.offer(dataItem, submTimeOutMilliSec, TimeUnit.MILLISECONDS)) {
				counterPreSubm.incrementAndGet();
			} else {
				throw new RejectedExecutionException("Submit queue timeout");
			}
		} else if(tmpFileConsumer != null) {
			tmpFileConsumer.submit(dataItem);
		} else {
			throw new RejectedExecutionException("Consuming failed due to internal error");
		}
	}
	/** Consumes the queue */
	@Override
	public final void run() {
		LOG.debug(
			LogUtil.MSG, "Determined submit queue capacity of {} for \"{}\"",
			volatileQueue.remainingCapacity(), getName()
		);
		T nextDataItem;
		try {
			while(volatileQueue.size() > 0 || !isShutdown.get()) {
				nextDataItem = volatileQueue.poll(submTimeOutMilliSec, TimeUnit.MILLISECONDS);
				if(nextDataItem != null) {
					submitSync(nextDataItem);
				}
			}
			LOG.debug(LogUtil.MSG, "{}: consuming finished", getName());
		} catch(final InterruptedException e) {
			LOG.debug(LogUtil.MSG, "{}: consuming interrupted", getName());
		} catch(final RejectedExecutionException e) {
			LOG.debug(LogUtil.MSG, "{}: consuming rejected", getName());
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Submit data item failure");
		} finally {
			shutdown();
			isAllSubm.set(true);
			if(tmpFileConsumer != null) {
				tmpFileConsumer.interrupt(); // delete the temp file
			}
		}
	}
	//
	protected abstract void submitSync(final T dataItem)
	throws InterruptedException, RemoteException;
	//
	@Override
	public void shutdown() {
		if(!isStarted.get()) {
			if(tmpFileConsumer == null) {
				throw new IllegalStateException("Not started yet, but shutdown is invoked");
			} else {
				LOG.debug(
					LogUtil.MSG, "{}: not started yet, trying to shutdown the persistent buffer",
					getName()
				);
				tmpFileConsumer.shutdown();
			}
		} else if(isShutdown.compareAndSet(false, true)) {
			final long countPreSubm = counterPreSubm.get();
			if(countPreSubm == 0) { // tmp file consumer has no consumed data items
				if(tmpFileConsumer != null) {
					try {
						tmpFileConsumer.close();
					} catch(final IOException e) {
						LogUtil.exception(
							LOG, Level.WARN, e, "Failed to close the temporary file consumer \"{}\"",
							tmpFileConsumer
						);
					} finally {
						tmpFileProducer = null; // dispose
						tmpFileConsumer.interrupt(); // delete the file
					}
				}
			}
			LOG.debug(LogUtil.MSG, "{}: consumed {} data items", getName(), counterPreSubm.get());
		}
	}
	//
	@Override
	public long getMaxCount() {
		return maxCount;
	}
	//
	@Override
	public synchronized void interrupt() {
		//
		shutdown();
		//
		if(tmpFileConsumer != null) {
			tmpFileConsumer.interrupt();
			LOG.debug(
				LogUtil.MSG, "{}: interrupted persistent buffer consumer \"{}\"", getName(),
				tmpFileConsumer
			);
		}
		//
		if(tmpFileProducer != null) {
			tmpFileProducer.interrupt();
			tmpFileProducer = null;
			LOG.debug(
				LogUtil.MSG, "{}: interrupted persistent buffer producer \"{}\"", getName(),
				tmpFileProducer
			);
		}
		//
		if(!super.isInterrupted()) {
			super.interrupt();
		}
	}
	//
	@Override
	public void close()
	throws IOException {
		shutdown();
		if(tmpFileConsumer != null) {
			tmpFileConsumer.close();
			tmpFileConsumer.interrupt(); // delete the temp file
		}
		final int dropCount = volatileQueue.size();
		if(dropCount > 0) {
			LOG.debug(LogUtil.MSG, "Dropped {} submit tasks", dropCount);
		}
		volatileQueue.clear(); // dispose
	}
}
